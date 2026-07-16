package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
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

    private final VisitaNavioRepositorio visitaRepositorio;
    private final SincronizadorStatusNavioPatioServico sincronizadorStatus;
    private final ExecucaoUnicaServico execucaoUnicaServico;

    public ReconciliacaoNavioPatioJob(VisitaNavioRepositorio visitaRepositorio,
                                      SincronizadorStatusNavioPatioServico sincronizadorStatus,
                                      ExecucaoUnicaServico execucaoUnicaServico) {
        this.visitaRepositorio = visitaRepositorio;
        this.sincronizadorStatus = sincronizadorStatus;
        this.execucaoUnicaServico = execucaoUnicaServico;
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.yard.reconciliacao-ms:60000}")
    @Transactional
    public void reconciliarVisitasAtivas() {
        boolean executado = execucaoUnicaServico.executarSeDisponivel(
                CHAVE_EXECUCAO,
                this::reconciliarVisitasAtivasSemBloqueio);
        if (!executado) {
            LOGGER.debug("Reconciliacao Navio x Patio ignorada porque outra instancia possui o bloqueio.");
        }
    }

    private void reconciliarVisitasAtivasSemBloqueio() {
        visitaRepositorio.findAllByOrderByEtaDesc().stream()
                .filter(visita -> visita.getFase() != null && !visita.getFase().terminal())
                .map(VisitaNavio::getId)
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
