package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;

public record ResultadoProcessamentoEdi(
        ProcessamentoEdiRespostaDto processamento,
        BayPlanRespostaDto bayPlan
) {
}
