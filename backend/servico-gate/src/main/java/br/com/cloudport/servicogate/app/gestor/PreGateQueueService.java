package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PreGateQueueService {

    private final GateFlowService gateFlowService;
    private final GateOperationsService gateOperationsService;

    public PreGateQueueService(
            GateFlowService gateFlowService,
            GateOperationsService gateOperationsService) {
        this.gateFlowService = gateFlowService;
        this.gateOperationsService = gateOperationsService;
    }

    public AgendamentoDTO confirmarChegadaAntecipada(Long agendamentoId) {
        AgendamentoDTO agendamento = gateFlowService.confirmarChegadaAntecipada(agendamentoId);
        if (agendamento.getGatePass() != null
                && agendamento.getGatePass().getId() != null
                && agendamento.getGatePass().getDataEntrada() == null) {
            gateOperationsService.adicionarNaFila(
                    agendamento.getGatePass().getId(),
                    GateQueueDirection.ENTRADA);
        }
        return agendamento;
    }
}
