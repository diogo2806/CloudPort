package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DispatchAutomaticoReconciliador {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final WorkQueuePatioRepositorio filaRepositorio;
    private final EtapaWorkInstructionServico etapaServico;
    private final SelecaoEquipamentoAuxiliarServico auxiliarServico;
    private final DispatchAutomaticoExecutor executor;
    private final NamedParameterJdbcTemplate jdbc;

    public DispatchAutomaticoReconciliador(
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            WorkQueuePatioRepositorio filaRepositorio,
            EtapaWorkInstructionServico etapaServico,
            SelecaoEquipamentoAuxiliarServico auxiliarServico,
            DispatchAutomaticoExecutor executor,
            NamedParameterJdbcTemplate jdbc) {
        this.ordemRepositorio = ordemRepositorio;
        this.filaRepositorio = filaRepositorio;
        this.etapaServico = etapaServico;
        this.auxiliarServico = auxiliarServico;
        this.executor = executor;
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${cloudport.dispatch.reconciliacao-ms:15000}")
    @Transactional
    public void reconciliar() {
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio.findByStatusOrdemInOrderByCriadoEmAsc(List.of(
                StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                StatusOrdemTrabalhoPatio.BLOQUEADA,
                StatusOrdemTrabalhoPatio.CONCLUIDA));
        for (OrdemTrabalhoPatio ordem : ordens) {
            etapaServico.sincronizarComVmt(ordem);
            processarEncerramento(ordem);
        }
    }

    private void processarEncerramento(OrdemTrabalhoPatio ordem) {
        StatusConfirmacaoVmt status = ordem.getStatusConfirmacaoVmt();
        if (status != StatusConfirmacaoVmt.CONCLUIDA && status != StatusConfirmacaoVmt.FALHA) {
            return;
        }
        String evento = status == StatusConfirmacaoVmt.CONCLUIDA ? "VMT_CONCLUSAO" : "VMT_FALHA";
        LocalDateTime momento = status == StatusConfirmacaoVmt.CONCLUIDA
                ? ordem.getVmtConcluidoEm() : ordem.getVmtFalhaEm();
        String assinatura = ordem.getId() + "|" + evento + "|" + momento;
        int inseridos = jdbc.update("""
                INSERT INTO gatilho_dispatch_processado (
                    ordem_trabalho_patio_id, evento, assinatura, processado_em, resultado
                ) VALUES (
                    :ordemId, :evento, :assinatura, CURRENT_TIMESTAMP, :resultado
                )
                ON CONFLICT (assinatura) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("ordemId", ordem.getId())
                .addValue("evento", evento)
                .addValue("assinatura", assinatura)
                .addValue("resultado", limitar(ordem.getResultadoVmt())));
        if (inseridos == 0) {
            return;
        }
        String statusDecisao = status == StatusConfirmacaoVmt.CONCLUIDA ? "CONCLUIDA" : "FALHA";
        jdbc.update("""
                UPDATE decisao_dispatch
                SET status = :status, atualizado_em = CURRENT_TIMESTAMP
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND status = 'ATRIBUIDA'
                """, new MapSqlParameterSource()
                .addValue("status", statusDecisao)
                .addValue("ordemId", ordem.getId()));
        auxiliarServico.devolverDaOrdem(ordem.getId(), "scheduler-automatico");
        if (ordem.getWorkQueueId() == null) {
            registrarResultado(assinatura, "Encerramento processado sem work queue associada.");
            return;
        }
        WorkQueuePatio fila = filaRepositorio.findById(ordem.getWorkQueueId()).orElse(null);
        if (fila == null || fila.getEquipamentoPatioId() == null) {
            registrarResultado(assinatura, "Encerramento processado sem CHE apto para encadeamento.");
            return;
        }
        try {
            boolean despachada = executor.despacharProxima(
                    fila.getId(), fila.getEquipamentoPatioId(), assinatura).isPresent();
            registrarResultado(assinatura, despachada
                    ? "Encerramento processado e proxima work instruction despachada."
                    : "Encerramento processado; modo nao automatico ou nenhuma instrucao elegivel.");
        } catch (RuntimeException exception) {
            registrarResultado(assinatura,
                    "Encerramento preservado; falha ao selecionar a proxima instrucao: "
                            + mensagem(exception));
        }
    }

    private void registrarResultado(String assinatura, String resultado) {
        jdbc.update("""
                UPDATE gatilho_dispatch_processado
                SET resultado = :resultado
                WHERE assinatura = :assinatura
                """, new MapSqlParameterSource()
                .addValue("assinatura", assinatura)
                .addValue("resultado", limitar(resultado)));
    }

    private String mensagem(RuntimeException exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return exception == null ? "erro nao identificado" : exception.getClass().getSimpleName();
        }
        return exception.getMessage().trim();
    }

    private String limitar(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.length() <= 1000 ? normalizado : normalizado.substring(0, 1000);
    }
}
