package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliacaoNavioPatioJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacaoNavioPatioJob.class);

    private final VisitaNavioRepositorio visitaRepositorio;
    private final SincronizadorStatusNavioPatioServico sincronizadorStatus;

    public ReconciliacaoNavioPatioJob(VisitaNavioRepositorio visitaRepositorio,
                                      SincronizadorStatusNavioPatioServico sincronizadorStatus) {
        this.visitaRepositorio = visitaRepositorio;
        this.sincronizadorStatus = sincronizadorStatus;
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.yard.reconciliacao-ms:60000}")
    public void reconciliarVisitasAtivas() {
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
