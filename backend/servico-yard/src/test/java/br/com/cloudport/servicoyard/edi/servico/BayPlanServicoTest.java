package br.com.cloudport.servicoyard.edi.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanContainerRepositorio;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("BayPlanServico - Ciclo de vida BAPLIE → COPRAR → COARRI")
class BayPlanServicoTest {

    @Mock
    private BayPlanRepositorio bayPlanRepositorio;
    @Mock
    private BayPlanContainerRepositorio containerRepositorio;
    @Mock
    private BayPlanPublicadorServico publicador;

    private BayPlanServico servico;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        servico = new BayPlanServico(bayPlanRepositorio, containerRepositorio, publicador);
    }

    // ── BAPLIE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BAPLIE: deve criar Bay Plan ATIVO quando não existe")
    void baplieDeveCriarAtivo() {
        BayPlan parsed = criarBayPlanTest("MSC_GULSEUM", "VOY001", 2);
        when(bayPlanRepositorio.findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                "MSC_GULSEUM", "VOY001")).thenReturn(Optional.empty());
        when(bayPlanRepositorio.save(any())).thenAnswer(inv -> {
            BayPlan bp = inv.getArgument(0);
            return bp;
        });

        BayPlanRespostaDto resultado = servico.processarBaplie(parsed);

        assertNotNull(resultado);
        assertEquals(StatusBayPlan.ATIVO, resultado.getStatus());
        assertEquals(2, resultado.getTotalContainers());
    }

    @Test
    @DisplayName("BAPLIE: deve substituir containers de Bay Plan existente")
    void baplieDeveSubstituirExistente() {
        BayPlan existente = criarBayPlanTest("MSC_GULSEUM", "VOY001", 1);
        existente.getContainers().get(0).setCodigoContainer("CONT_ANTIGO");

        BayPlan novoParsed = criarBayPlanTest("MSC_GULSEUM", "VOY001", 3);
        when(bayPlanRepositorio.findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                "MSC_GULSEUM", "VOY001")).thenReturn(Optional.of(existente));
        when(bayPlanRepositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BayPlanRespostaDto resultado = servico.processarBaplie(novoParsed);

        assertEquals(3, resultado.getTotalContainers());
        assertTrue(resultado.getContainers().stream()
                .noneMatch(c -> "CONT_ANTIGO".equals(c.getCodigoContainer())));
    }

    // ── COPRAR ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("COPRAR: deve atualizar posição de container existente")
    void coprarDeveAtualizarPosicao() {
        BayPlan existente = criarBayPlanTest("MSC_GULSEUM", "VOY001", 1);
        String codigo = existente.getContainers().get(0).getCodigoContainer();

        when(bayPlanRepositorio.findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                "MSC_GULSEUM", "VOY001")).thenReturn(Optional.of(existente));
        when(bayPlanRepositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BayPlanContainer atualizado = new BayPlanContainer();
        atualizado.setCodigoContainer(codigo);
        atualizado.setPosicaoBay(new PosicaoBay(20, 3, 4));
        atualizado.setTipoOperacao(TipoOperacaoBayPlan.CARREGAMENTO);

        BayPlanRespostaDto resultado = servico.processarCoprar(
                "MSC_GULSEUM", "VOY001", List.of(atualizado));

        assertEquals(StatusBayPlan.ATUALIZADO, resultado.getStatus());
        verify(publicador).publicarAtualizacaoCoprar(any(), any(), any());
    }

    @Test
    @DisplayName("COPRAR: deve adicionar container novo ao Bay Plan")
    void coprarDeveAdicionarContainerNovo() {
        BayPlan existente = criarBayPlanTest("MSC_GULSEUM", "VOY001", 1);
        when(bayPlanRepositorio.findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                "MSC_GULSEUM", "VOY001")).thenReturn(Optional.of(existente));
        when(bayPlanRepositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BayPlanContainer novo = new BayPlanContainer();
        novo.setCodigoContainer("NOVO_CONT_001");
        novo.setPosicaoBay(new PosicaoBay(30, 1, 2));
        novo.setTipoOperacao(TipoOperacaoBayPlan.CARREGAMENTO);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        BayPlanRespostaDto resultado = servico.processarCoprar(
                "MSC_GULSEUM", "VOY001", List.of(novo));

        assertEquals(2, resultado.getTotalContainers());
    }

    // ── COARRI ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("COARRI: deve marcar container como CONCLUIDO")
    void coarriDeveMarcarConcluido() {
        BayPlan existente = criarBayPlanTest("MSC_GULSEUM", "VOY001", 1);
        String codigo = existente.getContainers().get(0).getCodigoContainer();
        when(bayPlanRepositorio.findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                "MSC_GULSEUM", "VOY001")).thenReturn(Optional.of(existente));
        when(bayPlanRepositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BayPlanContainer conf = new BayPlanContainer();
        conf.setCodigoContainer(codigo);

        servico.processarCoarri("MSC_GULSEUM", "VOY001", List.of(conf));

        verify(publicador).publicarConfirmacaoCoarri(any(), any());
    }

    @Test
    @DisplayName("COARRI: deve definir status CONCLUIDO quando todos confirmados")
    void coarriDeveDefinirStatusConcluido() {
        BayPlan existente = criarBayPlanTest("MSC_GULSEUM", "VOY001", 2);
        List<BayPlanContainer> confirmacoes = new ArrayList<>();
        existente.getContainers().forEach(c -> {
            BayPlanContainer cf = new BayPlanContainer();
            cf.setCodigoContainer(c.getCodigoContainer());
            confirmacoes.add(cf);
        });
        when(bayPlanRepositorio.findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                "MSC_GULSEUM", "VOY001")).thenReturn(Optional.of(existente));
        when(bayPlanRepositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BayPlanRespostaDto resultado = servico.processarCoarri(
                "MSC_GULSEUM", "VOY001", confirmacoes);

        assertEquals(StatusBayPlan.CONCLUIDO, resultado.getStatus());
    }

    @Test
    @DisplayName("Deve lançar 404 para Bay Plan inexistente")
    void deveLancar404ParaInexistente() {
        when(bayPlanRepositorio.findById(999L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> servico.buscarPorId(999L));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private BayPlan criarBayPlanTest(String codigoNavio, String codigoViagem, int numContainers) {
        BayPlan bp = new BayPlan();
        bp.setCodigoNavio(codigoNavio);
        bp.setNomeNavio("MSC GULSEUM");
        bp.setCodigoViagem(codigoViagem);
        bp.setPortoCarga("BRSSZ");
        bp.setPortoDescarga("DEHAM");
        bp.setStatus(StatusBayPlan.RASCUNHO);
        bp.setOrigemMensagem("BAPLIE");
        for (int i = 0; i < numContainers; i++) {
            BayPlanContainer c = new BayPlanContainer();
            c.setCodigoContainer("CONT_TEST_" + String.format("%03d", i));
            c.setIsoCode("22G1");
            c.setPosicaoBay(new PosicaoBay(10 + i, 1, 2));
            c.setPortoDescarga("DEHAM");
            c.setTipoOperacao(TipoOperacaoBayPlan.DESCARGA);
            c.setStatusOperacao("PLANEJADO");
            bp.adicionarContainer(c);
        }
        return bp;
    }
}
