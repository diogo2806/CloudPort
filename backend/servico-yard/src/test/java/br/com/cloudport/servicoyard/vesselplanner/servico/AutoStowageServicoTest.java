package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.PerfilGeometriaNavioRepositorio;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutoStowageServico - atributos estruturados e geometria real")
class AutoStowageServicoTest {

    private AutoStowageServico servico;

    @BeforeEach
    void setup() {
        GeometriaNavioServico geometria = new GeometriaNavioServico(
                mock(PerfilGeometriaNavioRepositorio.class));
        servico = new AutoStowageServico(geometria);
    }

    @Test
    @DisplayName("Deve alocar reefer somente em slot com tomada real")
    void reeferAlocadoEmSlotReefer() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slotNormal = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slotReefer = criarSlot(plan, 2, 1, 1, TipoSlotNavio.REEFER);
        slotReefer.setTomadaReefer(true);
        plan.getSlots().addAll(List.of(slotNormal, slotReefer));

        BayPlanContainer container = criarContainer("REEFER001", "22G1", 15_000.0, "DEHAM");
        container.setReefer(true);
        container.setTemperaturaRequeridaC(-1.5);

        int alocados = servico.sugerirEstivagem(plan, List.of(container));

        assertEquals(1, alocados);
        assertEquals("REEFER001", slotReefer.getCodigoContainer());
        assertEquals(-1.5, slotReefer.getTemperaturaRequeridaC(), 0.001);
        assertNull(slotNormal.getCodigoContainer());
    }

    @Test
    @DisplayName("Não deve inferir reefer pelo ISO code")
    void naoDeveInferirReeferPeloIsoCode() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slotNormal = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slotReefer = criarSlot(plan, 2, 1, 1, TipoSlotNavio.REEFER);
        slotReefer.setTomadaReefer(true);
        plan.getSlots().addAll(List.of(slotNormal, slotReefer));

        BayPlanContainer container = criarContainer("NORMAL001", "22RE", 10_000.0, "DEHAM");

        servico.sugerirEstivagem(plan, List.of(container));

        assertEquals("NORMAL001", slotNormal.getCodigoContainer());
        assertNull(slotReefer.getCodigoContainer());
    }

    @Test
    @DisplayName("Deve copiar classe IMO, ONU e segregação para slot perigoso")
    void devePropagarCargaPerigosa() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slotNormal = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slotPerigoso = criarSlot(plan, 2, 1, 1, TipoSlotNavio.PERIGOSO);
        plan.getSlots().addAll(List.of(slotNormal, slotPerigoso));

        BayPlanContainer container = criarContainer("IMO001", "22G1", 12_000.0, "DEHAM");
        container.setPerigoso(true);
        container.setClasseImo("3");
        container.setNumeroOnu("1203");
        container.setGrupoSegregacao("INFLAMAVEL");

        int alocados = servico.sugerirEstivagem(plan, List.of(container));

        assertEquals(1, alocados);
        assertEquals("IMO001", slotPerigoso.getCodigoContainer());
        assertTrue(slotPerigoso.isPerigoso());
        assertEquals("3", slotPerigoso.getClasseImo());
        assertEquals("1203", slotPerigoso.getNumeroOnu());
        assertEquals("INFLAMAVEL", slotPerigoso.getGrupoSegregacao());
        assertNull(slotNormal.getCodigoContainer());
    }

    @Test
    @DisplayName("Deve manter classes perigosas incompatíveis afastadas")
    void deveAplicarSegregacaoDePerigosos() {
        EstivagemPlan plan = criarPlano();
        SlotNavio primeiroSlot = criarSlot(plan, 1, 1, 1, TipoSlotNavio.PERIGOSO);
        SlotNavio slotAdjacente = criarSlot(plan, 1, 2, 1, TipoSlotNavio.PERIGOSO);
        SlotNavio slotAfastado = criarSlot(plan, 2, 1, 1, TipoSlotNavio.PERIGOSO);
        plan.getSlots().addAll(List.of(primeiroSlot, slotAdjacente, slotAfastado));

        BayPlanContainer inflamavel = criarContainer("IMO003", "22G1", 10_000.0, "DEHAM");
        inflamavel.setPerigoso(true);
        inflamavel.setClasseImo("3");
        inflamavel.setNumeroOnu("1203");
        inflamavel.setGrupoSegregacao("A");

        BayPlanContainer corrosivo = criarContainer("IMO008", "22G1", 9_000.0, "DEHAM");
        corrosivo.setPerigoso(true);
        corrosivo.setClasseImo("8");
        corrosivo.setNumeroOnu("1760");
        corrosivo.setGrupoSegregacao("B");

        int alocados = servico.sugerirEstivagem(plan, List.of(inflamavel, corrosivo));

        assertEquals(2, alocados);
        assertEquals("IMO003", primeiroSlot.getCodigoContainer());
        assertNull(slotAdjacente.getCodigoContainer());
        assertEquals("IMO008", slotAfastado.getCodigoContainer());
    }

    @Test
    @DisplayName("Deve alocar OOG somente em slot dedicado e preservar dimensões")
    void deveAlocarOogEmSlotDedicado() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slotNormal = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slotOog = criarSlot(plan, 2, 1, 1, TipoSlotNavio.OOG);
        plan.getSlots().addAll(List.of(slotNormal, slotOog));

        BayPlanContainer container = criarContainer("OOG001", "42G1", 18_000.0, "DEHAM");
        container.setOog(true);
        container.setExcessoAlturaCm(35.0);

        int alocados = servico.sugerirEstivagem(plan, List.of(container));

        assertEquals(1, alocados);
        assertEquals("OOG001", slotOog.getCodigoContainer());
        assertTrue(slotOog.isOog());
        assertEquals(35.0, slotOog.getExcessoAlturaCm(), 0.001);
        assertNull(slotNormal.getCodigoContainer());
    }

    @Test
    @DisplayName("Deve usar VGM como peso operacional e posicionar contêiner pesado em tier baixo")
    void deveUsarVgmNoPlanejamento() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slotAlto = criarSlot(plan, 1, 1, 5, TipoSlotNavio.NORMAL);
        SlotNavio slotBaixo = criarSlot(plan, 1, 1, 2, TipoSlotNavio.NORMAL);
        plan.getSlots().addAll(List.of(slotAlto, slotBaixo));

        BayPlanContainer container = criarContainer("HEAVY001", "22G1", 18_000.0, "DEHAM");
        container.setPesoVgmKg(25_000.0);

        servico.sugerirEstivagem(plan, List.of(container));

        assertEquals("HEAVY001", slotBaixo.getCodigoContainer());
        assertEquals(25_000.0, slotBaixo.getPesoKg(), 0.001);
        assertEquals(25_000.0, slotBaixo.getPesoVgmKg(), 0.001);
        assertNull(slotAlto.getCodigoContainer());
    }

    @Test
    @DisplayName("Container para porto mais distante deve ir para tier mais baixo")
    void containerUltimoPortoEmTierBaixo() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slot1 = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        SlotNavio slot2 = criarSlot(plan, 1, 1, 2, TipoSlotNavio.NORMAL);
        plan.getSlots().addAll(List.of(slot1, slot2));

        BayPlanContainer deham = criarContainer("CONT_DEHAM", "22G1", 10_000.0, "DEHAM");
        BayPlanContainer nyc = criarContainer("CONT_NYC", "22G1", 10_000.0, "USNYC");

        servico.sugerirEstivagem(plan, List.of(deham, nyc));

        assertEquals("USNYC", slot1.getPortoDescarga());
        assertEquals("DEHAM", slot2.getPortoDescarga());
    }

    @Test
    @DisplayName("Deve ignorar slot restrito e respeitar comprimento admissível")
    void deveRespeitarRestricaoEComprimento() {
        EstivagemPlan plan = criarPlano();
        SlotNavio restrito = criarSlot(plan, 1, 1, 1, TipoSlotNavio.RESTRITO);
        restrito.setRestrito(true);
        SlotNavio somenteVinte = criarSlot(plan, 2, 1, 1, TipoSlotNavio.NORMAL);
        somenteVinte.setAceita40Pes(false);
        plan.getSlots().addAll(List.of(restrito, somenteVinte));

        BayPlanContainer container = criarContainer("FORTY001", "42G1", 10_000.0, "DEHAM");

        int alocados = servico.sugerirEstivagem(plan, List.of(container));

        assertEquals(0, alocados);
        assertNull(restrito.getCodigoContainer());
        assertNull(somenteVinte.getCodigoContainer());
    }

    @Test
    @DisplayName("Deve bloquear alocação que excede o limite real da pilha")
    void deveRespeitarLimitePilha() {
        EstivagemPlan plan = criarPlano();
        SlotNavio base = criarSlot(plan, 1, 1, 1, TipoSlotNavio.NORMAL);
        base.setCodigoContainer("BASE001");
        base.setPesoKg(20_000.0);
        base.setMaxPesoPilhaKg(25_000.0);
        SlotNavio topo = criarSlot(plan, 1, 1, 2, TipoSlotNavio.NORMAL);
        topo.setMaxPesoPilhaKg(25_000.0);
        plan.getSlots().addAll(List.of(base, topo));

        BayPlanContainer container = criarContainer("TOPO001", "22G1", 10_000.0, "DEHAM");

        assertEquals(0, servico.sugerirEstivagem(plan, List.of(container)));
        assertNull(topo.getCodigoContainer());
    }

    @Test
    @DisplayName("limparEstivagem deve remover todos os atributos operacionais")
    void limparEstivagem() {
        EstivagemPlan plan = criarPlano();
        SlotNavio slot = criarSlot(plan, 1, 1, 1, TipoSlotNavio.PERIGOSO);
        slot.setCodigoContainer("CONT001");
        slot.setPesoKg(10_000.0);
        slot.setPesoVgmKg(10_500.0);
        slot.setPerigoso(true);
        slot.setClasseImo("3");
        slot.setNumeroOnu("1203");
        plan.getSlots().add(slot);

        int removidos = servico.limparEstivagem(plan);

        assertEquals(1, removidos);
        assertNull(slot.getCodigoContainer());
        assertNull(slot.getPesoKg());
        assertNull(slot.getPesoVgmKg());
        assertFalse(slot.isPerigoso());
        assertNull(slot.getClasseImo());
        assertNull(slot.getNumeroOnu());
        assertEquals(EstadoCargaContainer.DESCONHECIDO, slot.getEstadoCarga());
    }

    @Test
    @DisplayName("Deve bloquear autoestivagem sem slots do perfil geométrico")
    void semSlotsBloqueiaAutoestivagem() {
        EstivagemPlan plan = criarPlano();
        BayPlanContainer container = criarContainer("CONT001", "22G1", 10_000.0, "DEHAM");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> servico.sugerirEstivagem(plan, List.of(container)));

        assertEquals("Plano sem slots provenientes do perfil geométrico", exception.getMessage());
    }

    private EstivagemPlan criarPlano() {
        EstivagemPlan plan = new EstivagemPlan();
        plan.setPerfilGeometriaId(10L);
        plan.setPerfilGeometriaVersao(1L);
        plan.setCondicaoCarregamento("OPERACIONAL");
        plan.setComprimentoLpp(300.0);
        plan.setBoca(45.0);
        plan.setCalado(14.0);
        plan.setDeslocamento(90_000.0);
        plan.setGm(1.5);
        plan.setTpc(75.0);
        plan.setLcb(150.0);
        return plan;
    }

    private SlotNavio criarSlot(EstivagemPlan plan,
                                  int bay,
                                  int row,
                                  int tier,
                                  TipoSlotNavio tipo) {
        SlotNavio slot = new SlotNavio();
        slot.setEstivagem(plan);
        slot.setBay(bay);
        slot.setRowBay(row);
        slot.setTier(tier);
        slot.setTipoSlot(tipo);
        slot.setAceita20Pes(true);
        slot.setAceita40Pes(true);
        slot.setAceita45Pes(true);
        slot.setMaxPesoKg(30_000.0);
        slot.setMaxPesoPilhaKg(100_000.0);
        slot.setStatusAlertas("OK");
        return slot;
    }

    private BayPlanContainer criarContainer(String codigo,
                                              String isoCode,
                                              Double peso,
                                              String portoDescarga) {
        BayPlanContainer container = new BayPlanContainer();
        container.setCodigoContainer(codigo);
        container.setIsoCode(isoCode);
        container.setPesoKg(peso);
        container.setEstadoCarga(EstadoCargaContainer.CHEIO);
        container.setPortoDescarga(portoDescarga);
        container.setTipoOperacao(TipoOperacaoBayPlan.DESCARGA);
        container.setStatusOperacao("PLANEJADO");
        return container;
    }
}
