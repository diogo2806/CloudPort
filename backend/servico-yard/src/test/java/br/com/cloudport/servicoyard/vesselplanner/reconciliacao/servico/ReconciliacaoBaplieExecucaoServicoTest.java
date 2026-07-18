package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusEstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto.ReconciliacaoBaplieExecucaoDTO.ReconciliacaoResposta;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.FonteDado;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.SeveridadeDivergencia;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.TipoDivergencia;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.ReconciliacaoBaplieExecucao;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.repositorio.DivergenciaReconciliacaoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.repositorio.ReconciliacaoBaplieExecucaoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReconciliacaoBaplieExecucaoServicoTest {

    @Mock
    private EstivagemPlanRepositorio planoRepositorio;
    @Mock
    private BayPlanRepositorio bayPlanRepositorio;
    @Mock
    private UnidadeInventarioRepositorio inventarioRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private ReconciliacaoBaplieExecucaoRepositorio reconciliacaoRepositorio;
    @Mock
    private DivergenciaReconciliacaoRepositorio divergenciaRepositorio;

    private ReconciliacaoBaplieExecucaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new ReconciliacaoBaplieExecucaoServico(
                planoRepositorio,
                bayPlanRepositorio,
                inventarioRepositorio,
                ordemRepositorio,
                reconciliacaoRepositorio,
                divergenciaRepositorio);
        when(reconciliacaoRepositorio.save(any(ReconciliacaoBaplieExecucao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveConcluirQuandoFontesConcordam() {
        EstivagemPlan plano = criarPlano(1, 2, 3);
        BayPlan bayPlan = criarBayPlan("ABCD1234567", 1, 2, 3, 10_000.0, "BRSSZ", true, false);
        SlotNavio slot = criarSlot(plano, "ABCD1234567", 1, 2, 3, 10_000.0, "BRSSZ", true, false);
        plano.getSlots().add(slot);
        UnidadeInventario unidade = criarUnidade("ABCD1234567", 10_000.0);

        prepararRepositorios(plano, bayPlan, List.of(unidade));

        ReconciliacaoResposta resposta = servico.reconciliar(1L, "planejador");

        assertEquals("CONCLUIDA", resposta.status());
        assertEquals(0, resposta.totalDivergencias());
        assertEquals(0, resposta.totalCriticasAbertas());
        assertFalse(resposta.bloqueiaOperacao());
    }

    @Test
    void deveBloquearQuandoSlotEReeferDivergem() {
        EstivagemPlan plano = criarPlano(1, 2, 3);
        BayPlan bayPlan = criarBayPlan("ABCD1234567", 1, 2, 3, 10_000.0, "BRSSZ", false, true);
        SlotNavio slot = criarSlot(plano, "ABCD1234567", 1, 2, 4, 10_000.0, "BRSSZ", false, false);
        plano.getSlots().add(slot);
        UnidadeInventario unidade = criarUnidade("ABCD1234567", 10_000.0);

        prepararRepositorios(plano, bayPlan, List.of(unidade));

        ReconciliacaoResposta resposta = servico.reconciliar(1L, "planejador");

        assertEquals("BLOQUEADA", resposta.status());
        assertEquals(2, resposta.totalCriticasAbertas());
        assertTrue(resposta.bloqueiaOperacao());
    }

    @Test
    void deveImpedirPublicacaoComDivergenciaCriticaAberta() {
        EstivagemPlan plano = criarPlano(1, 2, 3);
        ReconciliacaoBaplieExecucao reconciliacao = new ReconciliacaoBaplieExecucao();
        reconciliacao.setPlano(plano);
        reconciliacao.setBayPlanId(2L);
        reconciliacao.setVersaoPlano(plano.getVersao());
        reconciliacao.setSolicitante("planejador");

        DivergenciaReconciliacao divergencia = new DivergenciaReconciliacao();
        divergencia.setCodigoContainer("ABCD1234567");
        divergencia.setTipo(TipoDivergencia.SLOT);
        divergencia.setSeveridade(SeveridadeDivergencia.CRITICA);
        divergencia.setCampo("slot");
        divergencia.setFonteReferencia(FonteDado.BAPLIE);
        divergencia.setValorReferencia("010203");
        divergencia.setFonteDivergente(FonteDado.PLANO_APROVADO);
        divergencia.setValorDivergente("010204");
        reconciliacao.adicionarDivergencia(divergencia);

        when(planoRepositorio.findById(1L)).thenReturn(Optional.of(plano));
        when(reconciliacaoRepositorio.findTopByPlanoIdOrderByExecutadaEmDesc(1L))
                .thenReturn(Optional.of(reconciliacao));

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                () -> servico.exigirSemDivergenciasCriticas(1L));

        assertTrue(erro.getMessage().contains("divergência(s) crítica(s)"));
    }

    private void prepararRepositorios(
            EstivagemPlan plano,
            BayPlan bayPlan,
            List<UnidadeInventario> unidades) {
        when(planoRepositorio.findById(1L)).thenReturn(Optional.of(plano));
        when(bayPlanRepositorio.findById(2L)).thenReturn(Optional.of(bayPlan));
        when(inventarioRepositorio.findAllByOrderByIdentificacaoAsc()).thenReturn(unidades);
        when(ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(3L))
                .thenReturn(List.of());
    }

    private EstivagemPlan criarPlano(long id, long bayPlanId, long visitaNavioId) {
        EstivagemPlan plano = new EstivagemPlan();
        ReflectionTestUtils.setField(plano, "id", id);
        plano.setBayPlanId(bayPlanId);
        plano.setVisitaNavioId(visitaNavioId);
        plano.setCodigoNavio("IMO1234567");
        plano.setCodigoViagem("V001");
        plano.setStatus(StatusEstivagemPlan.RASCUNHO);
        return plano;
    }

    private BayPlan criarBayPlan(
            String codigo,
            int bay,
            int row,
            int tier,
            double peso,
            String porto,
            boolean perigoso,
            boolean reefer) {
        BayPlan bayPlan = new BayPlan();
        ReflectionTestUtils.setField(bayPlan, "id", 2L);
        BayPlanContainer container = new BayPlanContainer();
        container.setCodigoContainer(codigo);
        container.setPosicaoBay(new PosicaoBay(bay, row, tier));
        container.setPesoVgmKg(peso);
        container.setPortoDescarga(porto);
        container.setPerigoso(perigoso);
        container.setReefer(reefer);
        bayPlan.adicionarContainer(container);
        return bayPlan;
    }

    private SlotNavio criarSlot(
            EstivagemPlan plano,
            String codigo,
            int bay,
            int row,
            int tier,
            double peso,
            String porto,
            boolean perigoso,
            boolean reefer) {
        SlotNavio slot = new SlotNavio();
        slot.setEstivagem(plano);
        slot.setCodigoContainer(codigo);
        slot.setBay(bay);
        slot.setRowBay(row);
        slot.setTier(tier);
        slot.setPesoVgmKg(peso);
        slot.setPortoDescarga(porto);
        slot.setPerigoso(perigoso);
        slot.setReefer(reefer);
        return slot;
    }

    private UnidadeInventario criarUnidade(String codigo, double peso) {
        UnidadeInventario unidade = new UnidadeInventario();
        unidade.setIdentificacao(codigo);
        unidade.setEstado(UnidadeInventario.EstadoUnidade.ATIVA);
        unidade.setPesoBrutoKg(BigDecimal.valueOf(peso));
        return unidade;
    }
}
