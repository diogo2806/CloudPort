package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutoStowageServico - Algoritmo de auto-estivagem")
class AutoStowageServicoTest {

    private AutoStowageServico servico;

    @BeforeEach
    void setup() {
        servico = new AutoStowageServico();
    }

    @Test
    @DisplayName("Deve alocar container reefer apenas em slot REEFER")
    void reeferAlocadoEmSlotReefer() {
        EstivagemPlan plan = new EstivagemPlan();
        SlotNavio slotNormal = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slotReefer = criarSlot(plan, 2, 1, 1, TipoSlotNavio.REEFER);
        plan.getSlots().add(slotNormal);
        plan.getSlots().add(slotReefer);

        BayPlanContainer c = criarContainer("REEFER001", "22RE", 15000.0, "DEHAM");

        int alocados = servico.sugerirEstivagem(plan, List.of(c));

        assertEquals(1, alocados);
        assertEquals("REEFER001", slotReefer.getCodigoContainer());
        assertNull(slotNormal.getCodigoContainer());
    }

    @Test
    @DisplayName("Container pesado deve ser alocado em tier baixo (≤3)")
    void containerPesadoEmTierBaixo() {
        EstivagemPlan plan = new EstivagemPlan();
        SlotNavio slotAlto = criarSlot(plan, 1, 1, 5, TipoSlotNavio.NORMAL);
        SlotNavio slotBaixo = criarSlot(plan, 1, 1, 2, TipoSlotNavio.NORMAL);
        plan.getSlots().add(slotAlto);
        plan.getSlots().add(slotBaixo);

        BayPlanContainer c = criarContainer("HEAVY001", "22G1", 25000.0, "DEHAM");

        servico.sugerirEstivagem(plan, List.of(c));

        assertEquals("HEAVY001", slotBaixo.getCodigoContainer());
        assertNull(slotAlto.getCodigoContainer());
    }

    @Test
    @DisplayName("Container para porto mais distante deve ir para tier mais baixo (LIFO)")
    void containerUltimoPortoEmTierBaixo() {
        EstivagemPlan plan = new EstivagemPlan();
        SlotNavio slot1 = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slot2 = criarSlot(plan, 1, 1, 2, TipoSlotNavio.NORMAL);
        plan.getSlots().add(slot1);
        plan.getSlots().add(slot2);

        BayPlanContainer cDeham = criarContainer("CONT_DEHAM", "22G1", 10000.0, "DEHAM");
        BayPlanContainer cNyc = criarContainer("CONT_NYC", "22G1", 10000.0, "USNYC");

        servico.sugerirEstivagem(plan, List.of(cDeham, cNyc));

        assertEquals("USNYC", slot1.getPortoDescarga());
        assertEquals("DEHAM", slot2.getPortoDescarga());
    }

    @Test
    @DisplayName("limparEstivagem deve remover todos os containers")
    void limparEstivagem() {
        EstivagemPlan plan = new EstivagemPlan();
        SlotNavio s = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        s.setCodigoContainer("CONT001");
        s.setPesoKg(10000.0);
        plan.getSlots().add(s);

        int count = servico.limparEstivagem(plan);

        assertEquals(1, count);
        assertNull(s.getCodigoContainer());
        assertNull(s.getPesoKg());
    }

    @Test
    @DisplayName("Sem slots disponíveis retorna 0 alocações")
    void semSlotsNaoAloca() {
        EstivagemPlan plan = new EstivagemPlan();

        BayPlanContainer c = criarContainer("CONT001", "22G1", 10000.0, "DEHAM");
        int alocados = servico.sugerirEstivagem(plan, List.of(c));
        assertEquals(0, alocados);
    }

    private SlotNavio criarSlot(EstivagemPlan plan, int bay, int row, int tier, TipoSlotNavio tipo) {
        SlotNavio s = new SlotNavio();
        s.setEstivagem(plan);
        s.setBay(bay);
        s.setRowBay(row);
        s.setTier(tier);
        s.setTipoSlot(tipo);
        s.setMaxPesoKg(30000.0);
        s.setStatusAlertas("OK");
        return s;
    }

    private BayPlanContainer criarContainer(String codigo, String isoCode, Double peso, String portoDescarga) {
        BayPlanContainer c = new BayPlanContainer();
        c.setCodigoContainer(codigo);
        c.setIsoCode(isoCode);
        c.setPesoKg(peso);
        c.setPortoDescarga(portoDescarga);
        c.setTipoOperacao(TipoOperacaoBayPlan.DESCARGA);
        c.setStatusOperacao("PLANEJADO");
        return c;
    }
}
