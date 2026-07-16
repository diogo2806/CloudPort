package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EdiFilaProcessamentoServico {

    private static final int MAXIMO_TENTATIVAS = 5;
    private static final Duration TEMPO_LIMITE_PROCESSAMENTO = Duration.ofMinutes(5);
    private static final Duration RETENTATIVA_BASE = Duration.ofSeconds(30);
    private static final Duration RETENTATIVA_MAXIMA = Duration.ofMinutes(15);

    private final ProcessamentoEdiRepositorio repositorio;

    public EdiFilaProcessamentoServico(ProcessamentoEdiRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public List<Long> reivindicarPendentes() {
        LocalDateTime agora = LocalDateTime.now();
        List<ProcessamentoEdi> pendentes = repositorio
                .findTop20ByStatusInAndProximaTentativaEmLessThanEqualOrderByCriadoEmAsc(
                        List.of(
                                StatusProcessamentoEdi.RECEBIDO,
                                StatusProcessamentoEdi.AGUARDANDO_RETENTATIVA),
                        agora);
        for (ProcessamentoEdi processamento : pendentes) {
            if (processamento.getStatus() == StatusProcessamentoEdi.AGUARDANDO_RETENTATIVA) {
                processamento.setTentativa(processamento.getTentativa() + 1);
            }
            processamento.setStatus(StatusProcessamentoEdi.PROCESSANDO);
            processamento.setProcessandoDesde(agora);
            processamento.setProximaTentativaEm(null);
        }
        return pendentes.stream().map(ProcessamentoEdi::getId).toList();
    }

    @Transactional
    public int recuperarProcessamentosTravados() {
        LocalDateTime limite = LocalDateTime.now().minus(TEMPO_LIMITE_PROCESSAMENTO);
        List<ProcessamentoEdi> travados = repositorio
                .findTop100ByStatusAndProcessandoDesdeBeforeOrderByProcessandoDesdeAsc(
                        StatusProcessamentoEdi.PROCESSANDO,
                        limite);
        LocalDateTime agora = LocalDateTime.now();
        for (ProcessamentoEdi processamento : travados) {
            processamento.setStatus(StatusProcessamentoEdi.AGUARDANDO_RETENTATIVA);
            processamento.setProcessandoDesde(null);
            processamento.setProximaTentativaEm(agora);
            processamento.setMotivoRejeicao(
                    "Processamento interrompido antes da confirmacao; mensagem recolocada na fila.");
        }
        return travados.size();
    }

    @Transactional
    public void registrarFalha(Long processamentoId, RuntimeException erro) {
        ProcessamentoEdi processamento = repositorio.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Processamento EDI nao encontrado: " + processamentoId));
        if (processamento.getStatus() == StatusProcessamentoEdi.CONCLUIDO) {
            return;
        }

        processamento.setProcessandoDesde(null);
        processamento.setMotivoRejeicao(limitar(mensagemRaiz(erro), 2000));
        if (processamento.getTentativa() >= MAXIMO_TENTATIVAS) {
            processamento.setStatus(StatusProcessamentoEdi.QUARENTENA);
            processamento.setProximaTentativaEm(null);
            return;
        }

        processamento.setStatus(StatusProcessamentoEdi.AGUARDANDO_RETENTATIVA);
        processamento.setProximaTentativaEm(
                LocalDateTime.now().plus(calcularEspera(processamento.getTentativa())));
    }

    private Duration calcularEspera(int tentativaAtual) {
        long multiplicador = 1L << Math.min(Math.max(tentativaAtual - 1, 0), 10);
        Duration calculada = RETENTATIVA_BASE.multipliedBy(multiplicador);
        return calculada.compareTo(RETENTATIVA_MAXIMA) > 0 ? RETENTATIVA_MAXIMA : calculada;
    }

    private String mensagemRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return StringUtils.hasText(atual.getMessage())
                ? atual.getMessage()
                : erro.getClass().getSimpleName();
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }
}
