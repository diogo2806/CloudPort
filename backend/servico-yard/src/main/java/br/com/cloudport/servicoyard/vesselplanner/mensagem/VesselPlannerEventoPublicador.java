package br.com.cloudport.servicoyard.vesselplanner.mensagem;

import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SlotNavioDto;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class VesselPlannerEventoPublicador {

    private final SimpMessagingTemplate template;

    public VesselPlannerEventoPublicador(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publicarAtualizacaoSlot(Long planId, SlotNavioDto slot, EstabilidadeDto estabilidade) {
        template.convertAndSend(
                "/topico/vessel-planner/" + planId,
                Map.of(
                        "tipo", "SLOT_ATUALIZADO",
                        "slot", slot,
                        "estabilidade", estabilidade));
    }

    public void publicarViolacaoDetectada(Long planId, Object violacao) {
        template.convertAndSend(
                "/topico/vessel-planner/" + planId,
                Map.of("tipo", "VIOLACAO_DETECTADA", "violacao", violacao));
    }

    public void publicarEstabilidadeAtualizada(Long planId, EstabilidadeDto estabilidade) {
        template.convertAndSend(
                "/topico/vessel-planner/" + planId,
                Map.of("tipo", "ESTABILIDADE_ATUALIZADA", "estabilidade", estabilidade));
    }
}
