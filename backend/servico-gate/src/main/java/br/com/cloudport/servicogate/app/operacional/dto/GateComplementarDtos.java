package br.com.cloudport.servicogate.app.operacional.dto;

import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AccessRuleDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BillOfLadingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.OrderDTO;
import java.util.List;

public final class GateComplementarDtos {

    private GateComplementarDtos() {
    }

    public record GateComplementarDTO(
            List<BillOfLadingDTO> billsOfLading,
            List<AccessRuleDTO> regrasAcesso) {
    }

    public record VinculoBillOfLadingDTO(
            OrderDTO ordem,
            BillOfLadingDTO billOfLading) {
    }
}