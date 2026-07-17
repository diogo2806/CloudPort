package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.PerfilGeometriaNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotPerfilNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusPerfilGeometriaNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.PerfilGeometriaNavioRepositorio;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeometriaNavioServico")
class GeometriaNavioServicoTest {

    private PerfilGeometriaNavioRepositorio repositorio;
    private GeometriaNavioServico servico;

    @BeforeEach
    void setup() {
        repositorio = mock(PerfilGeometriaNavioRepositorio.class);
        servico = new GeometriaNavioServico(repositorio);
    }

    @Test
    @DisplayName("Deve criar somente os slots do perfil e preservar a posição BAPLIE")
    void devePreservarPosicaoBayPlan() {
        PerfilGeometriaNavio perfil = criarPerfil();
        perfil.adicionarSlot(criarSlotPerfil(3, 4, 82, TipoSlotNavio.NORMAL));
        perfil.adicionarSlot(criarSlotPerfil(3, 6, 82, TipoSlotNavio.NORMAL));

        EstivagemPlan plan = criarPlano();
        servico.aplicarPerfil(plan, perfil);

        BayPlanContainer container = criarContainer("MSCU1234567", 3, 4, 82);
        servico.posicionarConteineresImportados(plan, List.of(container));

        assertEquals(2, plan.getSlots().size());
        SlotNavio ocupado = plan.getSlots().stream()
                .filter(slot -> "MSCU1234567".equals(slot.getCodigoContainer()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, ocupado.getBay());
        assertEquals(4, ocupado.getRowBay());
        assertEquals(82, ocupado.getTier());
        assertEquals(perfil.getId(), plan.getPerfilGeometriaId());
        assertEquals(perfil.getVersaoPerfil(), plan.getPerfilGeometriaVersao());
        assertNotNull(plan.getComprimentoLpp());
    }

    @Test
    @DisplayName("Deve bloquear operação quando não houver perfil aprovado")
    void deveBloquearSemPerfilAprovado() {
        when(repositorio.findFirstByCodigoNavioIgnoreCaseAndStatusOrderByVersaoPerfilDesc(
                "NAVIO-01",
                StatusPerfilGeometriaNavio.APROVADO)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> servico.carregarPerfilAprovado("NAVIO-01"));
    }

    @Test
    @DisplayName("Deve bloquear posição BAPLIE restrita")
    void deveBloquearPosicaoRestrita() {
        PerfilGeometriaNavio perfil = criarPerfil();
        SlotPerfilNavio slot = criarSlotPerfil(5, 2, 84, TipoSlotNavio.RESTRITO);
        slot.setRestrito(true);
        slot.setMotivoRestricao("estrutura do hatch cover");
        perfil.adicionarSlot(slot);

        EstivagemPlan plan = criarPlano();
        servico.aplicarPerfil(plan, perfil);
        BayPlanContainer container = criarContainer("MSCU7654321", 5, 2, 84);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> servico.posicionarConteineresImportados(plan, List.of(container)));
        assertTrue(exception.getMessage().contains("Slot restrito"));
    }

    @Test
    @DisplayName("Deve bloquear peso acumulado acima do limite real da pilha")
    void deveBloquearSobrepesoPilha() {
        PerfilGeometriaNavio perfil = criarPerfil();
        SlotPerfilNavio base = criarSlotPerfil(7, 1, 82, TipoSlotNavio.NORMAL);
        base.setMaxPesoPilhaKg(25_000.0);
        SlotPerfilNavio topo = criarSlotPerfil(7, 1, 84, TipoSlotNavio.NORMAL);
        topo.setMaxPesoPilhaKg(25_000.0);
        perfil.adicionarSlot(base);
        perfil.adicionarSlot(topo);

        EstivagemPlan plan = criarPlano();
        servico.aplicarPerfil(plan, perfil);
        BayPlanContainer primeiro = criarContainer("MSCU0000001", 7, 1, 82);
        primeiro.setPesoKg(15_000.0);
        BayPlanContainer segundo = criarContainer("MSCU0000002", 7, 1, 84);
        segundo.setPesoKg(15_000.0);

        assertThrows(IllegalStateException.class,
                () -> servico.posicionarConteineresImportados(plan, List.of(primeiro, segundo)));
    }

    private PerfilGeometriaNavio criarPerfil() {
        PerfilGeometriaNavio perfil = new PerfilGeometriaNavio();
        perfil.setId(15L);
        perfil.setCodigoNavio("NAVIO-01");
        perfil.setVersaoPerfil(4L);
        perfil.setStatus(StatusPerfilGeometriaNavio.APROVADO);
        perfil.setCondicaoCarregamento("CHEGADA");
        perfil.setComprimentoLpp(300.0);
        perfil.setBoca(45.0);
        perfil.setCalado(14.0);
        perfil.setDeslocamento(90_000.0);
        perfil.setGm(1.5);
        perfil.setTpc(75.0);
        perfil.setLcb(150.0);
        return perfil;
    }

    private SlotPerfilNavio criarSlotPerfil(int bay,
                                              int row,
                                              int tier,
                                              TipoSlotNavio tipo) {
        SlotPerfilNavio slot = new SlotPerfilNavio();
        slot.setBay(bay);
        slot.setRowBay(row);
        slot.setTier(tier);
        slot.setTipoSlot(tipo);
        slot.setAceita20Pes(true);
        slot.setAceita40Pes(true);
        slot.setAceita45Pes(true);
        slot.setMaxPesoKg(30_000.0);
        slot.setMaxPesoPilhaKg(100_000.0);
        return slot;
    }

    private EstivagemPlan criarPlano() {
        EstivagemPlan plan = new EstivagemPlan();
        plan.setCodigoNavio("NAVIO-01");
        plan.setCodigoViagem("V001");
        return plan;
    }

    private BayPlanContainer criarContainer(String codigo, int bay, int row, int tier) {
        BayPlanContainer container = new BayPlanContainer();
        container.setCodigoContainer(codigo);
        container.setIsoCode("22G1");
        container.setPosicaoBay(new PosicaoBay(bay, row, tier));
        container.setPesoKg(10_000.0);
        container.setEstadoCarga(EstadoCargaContainer.CHEIO);
        return container;
    }
}
