package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardPorta;
import br.com.cloudport.serviconaviosiderurgico.cliente.WorkQueueValidacaoYardDto;
import br.com.cloudport.serviconaviosiderurgico.dominio.SequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusSequenciaGuindaste;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliacaoSequenciaGuindasteJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacaoSequenciaGuindasteJob.class);

    private final SequenciaGuindasteServico sequenciaServico;
    private final ConsultaWorkQueueYardPorta consultaWorkQueueYard;

    public ReconciliacaoSequenciaGuindasteJob(
            SequenciaGuindasteServico sequenciaServico,
            ConsultaWorkQueueYardPorta consultaWorkQueueYard
    ) {
        this.sequenciaServico = sequenciaServico;
        this.consultaWorkQueueYard = consultaWorkQueueYard;
    }

    @Scheduled(
            initialDelayString = "${cloudport.crane-sequence.reconciliation.initial-delay-ms:60000}",
            fixedDelayString = "${cloudport.crane-sequence.reconciliation.fixed-delay-ms:300000}")
    public void reconciliar() {
        Map<Long, List<WorkQueueValidacaoYardDto>> filasPorVisita = new HashMap<>();
        for (SequenciaGuindaste sequencia : sequenciaServico.listarParaReconciliacao()) {
            try {
                reconciliar(sequencia, filasPorVisita);
            } catch (RuntimeException ex) {
                LOGGER.warn("Falha ao reconciliar movementId={} com o Yard: {}",
                        sequencia.getMovementId(), ex.getMessage());
            }
        }
    }

    private void reconciliar(
            SequenciaGuindaste sequencia,
            Map<Long, List<WorkQueueValidacaoYardDto>> filasPorVisita
    ) {
        Long visitaId = numero(sequencia.getVesselVisitId());
        Long workQueueId = numero(sequencia.getLoadUnitId());
        if (visitaId == null || workQueueId == null) {
            sequenciaServico.registrarAlertaReconciliacao(
                    sequencia.getMovementId(),
                    "Nao foi possivel reconciliar automaticamente: vesselVisitId e loadUnitId devem identificar a visita e a work queue do Yard.");
            return;
        }

        List<WorkQueueValidacaoYardDto> filas = filasPorVisita.computeIfAbsent(
                visitaId,
                consultaWorkQueueYard::listarParaValidacaoPlano);
        Optional<WorkQueueValidacaoYardDto> fila = filas.stream()
                .filter(item -> workQueueId.equals(item.getId()))
                .findFirst();
        if (fila.isEmpty()) {
            sequenciaServico.registrarAlertaReconciliacao(
                    sequencia.getMovementId(),
                    "Work queue " + workQueueId + " nao foi encontrada no Yard para a visita " + visitaId + ".");
            return;
        }

        WorkQueueValidacaoYardDto yard = fila.get();
        if (sequencia.getStatus() == StatusSequenciaGuindaste.STARTED && !"ATIVA".equals(yard.getStatus())) {
            sequenciaServico.registrarAlertaReconciliacao(
                    sequencia.getMovementId(),
                    "Movimento iniciado, mas a work queue " + workQueueId + " esta no estado " + yard.getStatus() + " no Yard.");
        }
        if (sequencia.getStatus() == StatusSequenciaGuindaste.FINISHED && yard.getTotalOrdensDispatchaveis() > 0) {
            sequenciaServico.registrarAlertaReconciliacao(
                    sequencia.getMovementId(),
                    "Movimento finalizado, mas a work queue " + workQueueId + " ainda possui "
                            + yard.getTotalOrdensDispatchaveis() + " ordem(ns) dispatchavel(is) no Yard.");
        }
    }

    private Long numero(String valor) {
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
