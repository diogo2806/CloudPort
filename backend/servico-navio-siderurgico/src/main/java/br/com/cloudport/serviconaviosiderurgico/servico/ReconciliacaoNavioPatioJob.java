package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "cloudport.runtime.jobs-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReconciliacaoNavioPatioJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacaoNavioPatioJob.class);
    private static final String CHAVE_EXECUCAO = "cloudport:navio-patio:reconciliacao";
    private static final List<StatusIntegracaoPatio> STATUS_PENDENTES = List.of(
            StatusIntegracaoPatio.ORDEM_GERADA,
            StatusIntegracaoPatio.EM_EXECUCAO,
            StatusIntegracaoPatio.ERRO
    );

    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final SincronizadorStatusNavioPatioServico sincronizadorStatus;
    private final ExecucaoUnicaServico execucaoUnicaServico;

    public ReconciliacaoNavioPatioJob(ItemOperacaoNavioRepositorio itemRepositorio,
                                      SincronizadorStatusNavioPatioServico sincronizadorStatus,
                                      ExecucaoUnicaServico execucaoUnicaServico) {
        this.itemRepositorio = itemRepositorio;
        this.sincronizadorStatus = sincronizadorStatus;
        this.execucaoUnicaServico = execucaoUnicaServico;
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.yard.reconciliacao-ms:60000}")
    @Transactional
    public void reconciliarVisitasAtivas() {
        boolean executado = execucaoUnicaServico.executarSeDisponivel(
                CHAVE_EXECUCAO,
                this::reconciliarPendenciasSemBloqueio);
        if (!executado) {
            LOGGER.debug("Reconciliacao Navio x Patio ignorada porque outra instancia possui o bloqueio.");
        }
    }

    private void reconciliarPendenciasSemBloqueio() {
        itemRepositorio.findTop100ByStatusIntegracaoPatioInOrderByAtualizadoEmAsc(STATUS_PENDENTES)
                .stream()
                .map(ItemOperacaoNavio::getVisitaNavio)
                .filter(Objects::nonNull)
                .map(visita -> visita.getId())
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::sincronizarComTratamento);
    }

    private void sincronizarComTratamento(Long visitaNavioId) {
        try {
            int alterados = sincronizadorStatus.sincronizarStatus(visitaNavioId);
            if (alterados > 0) {
                LOGGER.info("Reconciliacao Navio x Patio atualizou {} item(ns) da visita {}", alterados, visitaNavioId);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Falha na reconciliacao Navio x Patio da visita {}: {}", visitaNavioId, ex.getMessage());
        }
    }
}
