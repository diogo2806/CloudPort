package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.vesselplanner.dto.RestowAnaliseDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RestowCalculadorServico - Análise de re-estivamento")
class RestowCalculadorServicoTest {

    private RestowCalculadorServico servico;

    @BeforeEach
    void setup() {
        servico = new RestowCalculadorServico();
    }

    @Test
    @DisplayName("Plano sem violação de ordem de porto não deve ter restow")
    void semViolacaoNaoTemRestow() {
        EstivagemPlan plan = new EstivagemPlan();
        plan.getSlots().add(criarSlot(plan, 1, 1, 1, "USNYC"));
        plan.getSlots().add(criarSlot(plan, 1, 1, 2, "DEHAM"));

        RestowAnaliseDto dto = servico.analisar(plan);
        assertEquals(0, dto.getTotalRestows());
    }

    @Test
    @DisplayName("Container com porto anterior abaixo deve gerar restow")
    void violacaoOrdemPortoGeraRestow() {
        EstivagemPlan plan = new EstivagemPlan();
        plan.getSlots().add(criarSlot(plan, 1, 1, 1, "DEHAM"));
        plan.getSlots().add(criarSlot(plan, 1, 1, 2, "USNYC"));

        RestowAnaliseDto dto = servico.analisar(plan);
        assertEquals(1, dto.getTotalRestows());
        assertEquals("DEHAM", plan.getSlots().get(0).getPortoDescarga());
    }

    @Test
    @DisplayName("Pilha sem containers não deve gerar restow")
    void slotsVaziosNaoGeram() {
        EstivagemPlan plan = new EstivagemPlan();
        SlotNavio s1 = criarSlot(plan, 1, 1, 1, null);
        s1.setCodigoContainer(null);
        s1.setPortoDescarga(null);
        plan.getSlots().add(s1);

        RestowAnaliseDto dto = servico.analisar(plan);
        assertEquals(0, dto.getTotalRestows());
    }

    @Test
    @DisplayName("Múltiplas violações em stacks separados")
    void multiplosStacksComViolacoes() {
        EstivagemPlan plan = new EstivagemPlan();
        plan.getSlots().add(criarSlot(plan, 1, 1, 1, "DEHAM"));
        plan.getSlots().add(criarSlot(plan, 1, 1, 2, "USNYC"));
        plan.getSlots().add(criarSlot(plan, 2, 1, 1, "BRSSZ"));
        plan.getSlots().add(criarSlot(plan, 2, 1, 2, "DEHAM"));

        RestowAnaliseDto dto = servico.analisar(plan);
        assertEquals(2, dto.getTotalRestows());
    }

    private SlotNavio criarSlot(EstivagemPlan plan, int bay, int row, int tier, String portoDescarga) {
        SlotNavio s = new SlotNavio();
        s.setEstivagem(plan);
        s.setBay(bay);
        s.setRowBay(row);
        s.setTier(tier);
        s.setTipoSlot(TipoSlotNavio.NORMAL);
        s.setCodigoContainer(portoDescarga != null ? "CONT_" + bay + row + tier : null);
        s.setPortoDescarga(portoDescarga);
        s.setStatusAlertas("OK");
        return s;
    }
}
