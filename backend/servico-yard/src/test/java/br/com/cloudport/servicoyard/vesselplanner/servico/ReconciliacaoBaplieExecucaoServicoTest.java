package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.dto.ReconciliacaoBaplieExecucaoDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.DivergenciaReconciliacaoSlot;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SeveridadeDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.DivergenciaReconciliacaoSlotRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliacaoBaplieExecucaoServicoTest {

    @Mock
    private EstivagemPlanRepositorio planRepositorio;
    @Mock
    private BayPlanRepositorio bayPlanRepositorio;
    @Mock
    private UnidadeInventarioRepositorio unidadeInventarioRepositorio;
    @Mock
    private DivergenciaReconciliacaoSlotRepositorio divergenciaRepositorio;
    @Mock
    private SlotNavioRepositorio slotRepositorio;

    private ReconciliacaoBaplieExecucaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new ReconciliacaoBaplieExecucaoServico(
                planRepositorio,
                bayPlanRepositorio,
                unidadeInventarioRepositorio,
                divergenciaRepositorio,
                slotRepositorio);
    }

    @Test
    void devePersistirDivergenciaCriticaEntreBaplieEPlano() {
        EstivagemPlan plan = new EstivagemPlan();
        plan.setId(1L);
        plan.setBayPlanId(10L);

        SlotNavio slot = new SlotNavio();
        slot.setId(100L);
        slot.setEstivagem(plan);
        slot.setBay(1);
        slot.setRowBay(2);
        slot.setTier(4);
        slot.setCodigoContainer("CONT0000001");
        slot.setIsoCode("22G1");
        slot.setPesoKg(12000.0);
        plan.setSlots(new ArrayList<>(List.of(slot)));

        BayPlanContainer container = new BayPlanContainer();
        container.setCodigoContainer("CONT0000001");
        container.setIsoCode("22G1");
        container.setPesoKg(12000.0);
        container.setPosicaoBay(new PosicaoBay(3, 2, 4));
        container.setTipoOperacao(TipoOperacaoBayPlan.CARREGAMENTO);
        container.setStatusOperacao("PLANEJADO");

        BayPlan bayPlan = new BayPlan();
        bayPlan.setCodigoNavio("IMO1234567");
        bayPlan.setCodigoViagem("V001");
        bayPlan.setStatus(StatusBayPlan.ATIVO);
        bayPlan.adicionarContainer(container);

        when(planRepositorio.findById(1L)).thenReturn(Optional.of(plan));
        when(bayPlanRepositorio.findById(10L)).thenReturn(Optional.of(bayPlan));
        when(unidadeInventarioRepositorio.findByIdentificacaoIgnoreCase("CONT0000001"))
                .thenReturn(Optional.empty());
        when(divergenciaRepositorio.findByEstivagemIdOrderByCriadoEmAsc(1L))
                .thenReturn(List.of());
        when(divergenciaRepositorio.saveAll(anyList()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(slotRepositorio.saveAll(anyList()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ReconciliacaoBaplieExecucaoDto resposta = servico.reconciliar(1L);

        assertEquals(2, resposta.getTotalDivergencias());
        assertEquals(1, resposta.getCriticasAbertas());
        assertTrue(resposta.getDivergencias().stream()
                .anyMatch(item -> item.getTipo()
                        == TipoDivergenciaReconciliacao.POSICAO_PLANO_DIVERGENTE
                        && item.getSeveridade()
                        == SeveridadeDivergenciaReconciliacao.CRITICA));
        assertTrue(resposta.getDivergencias().stream()
                .anyMatch(item -> item.getTipo()
                        == TipoDivergenciaReconciliacao.AUSENTE_NO_INVENTARIO));
    }

    @Test
    void deveBloquearPublicacaoComDivergenciaCriticaAberta() {
        when(divergenciaRepositorio.countByEstivagemIdAndSeveridadeAndStatus(
                1L,
                SeveridadeDivergenciaReconciliacao.CRITICA,
                StatusDivergenciaReconciliacao.ABERTA))
                .thenReturn(2L);

        IllegalStateException excecao = assertThrows(
                IllegalStateException.class,
                () -> servico.exigirSemDivergenciasCriticas(1L));

        assertTrue(excecao.getMessage().contains("2 divergência(s) crítica(s)"));
    }
}
