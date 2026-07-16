package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoGuindasteVisita;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.AlocacaoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ProdutividadeCaisDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoGuindasteVisitaRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuayBerthCraneServicoTest {

    @Mock private VisitaNavioServico visitaNavioServico;
    @Mock private ItemOperacaoNavioRepositorio itemRepositorio;
    @Mock private PlanoGuindasteVisitaRepositorio planoRepositorio;

    private QuayBerthCraneServico servico;

    @BeforeEach
    void configurar() {
        servico = new QuayBerthCraneServico(visitaNavioServico, itemRepositorio, planoRepositorio);
    }

    @Test
    void deveCalcularProdutividadeComMovimentosReaisDaVisita() {
        LocalDateTime agora = LocalDateTime.now();
        VisitaNavio visita = visita(agora.minusHours(2));
        ItemOperacaoNavio operado = item(10, 1, StatusItemCarga.OPERADO);
        ItemOperacaoNavio pendente = item(5, 1, StatusItemCarga.PLANEJADO);
        PlanoGuindasteVisita plano = plano(99L, agora.minusHours(3), agora.plusHours(1));

        when(visitaNavioServico.buscarEntidade(1L)).thenReturn(visita);
        when(itemRepositorio.findByVisitaNavioId(1L)).thenReturn(List.of(operado, pendente));
        when(planoRepositorio.findByVisitaNavioIdOrderBySequenciaAsc(1L)).thenReturn(List.of(plano));

        ProdutividadeCaisDTO produtividade = servico.obterProdutividadeCais(1L);

        assertEquals(15, produtividade.movimentosPlanejados());
        assertEquals(10, produtividade.movimentosRealizados());
        assertEquals(5, produtividade.movimentosPendentes());
        assertEquals(1, produtividade.guindastes().size());
        assertEquals(10, produtividade.guindastes().get(0).movimentosRealizados());
        assertTrue(produtividade.produtividadeAtualMovimentosHora().compareTo(new BigDecimal("4.90")) >= 0);
        assertTrue(produtividade.produtividadeAtualMovimentosHora().compareTo(new BigDecimal("5.10")) <= 0);
    }

    @Test
    void deveRejeitarSobreposicaoDoMesmoGuindaste() {
        LocalDateTime inicio = LocalDateTime.now().plusHours(1);
        VisitaNavio visita = visita(null);
        List<AlocacaoGuindasteDTO> alocacoes = List.of(
                alocacao("QC-01", 1, 1, inicio, inicio.plusHours(2)),
                alocacao("QC-01", 1, 2, inicio.plusHours(1), inicio.plusHours(3)));
        ComandoPlanoGuindasteDTO comando = new ComandoPlanoGuindasteDTO(
                "B01",
                StatusPlanoGuindaste.PUBLICADO,
                alocacoes,
                "planejador",
                null);

        when(visitaNavioServico.buscarEntidade(1L)).thenReturn(visita);
        when(itemRepositorio.findByVisitaNavioId(1L)).thenReturn(List.of(item(20, 1, StatusItemCarga.PLANEJADO)));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> servico.salvarPlano(1L, comando));

        assertTrue(erro.getMessage().contains("sobrepostas"));
    }

    @Test
    void deveSubstituirPlanoERegistrarEventoOperacional() {
        LocalDateTime inicio = LocalDateTime.now().plusHours(1);
        VisitaNavio visita = visita(null);
        ComandoPlanoGuindasteDTO comando = new ComandoPlanoGuindasteDTO(
                "B01",
                StatusPlanoGuindaste.PUBLICADO,
                List.of(alocacao("QC-01", 1, 1, inicio, inicio.plusHours(2))),
                "planejador",
                "Plano inicial");

        when(visitaNavioServico.buscarEntidade(1L)).thenReturn(visita);
        when(itemRepositorio.findByVisitaNavioId(1L)).thenReturn(List.of(item(10, 1, StatusItemCarga.PLANEJADO)));
        when(planoRepositorio.saveAll(anyList())).thenAnswer(invocacao -> invocacao.getArgument(0));

        PlanoGuindasteDTO resposta = servico.salvarPlano(1L, comando);

        verify(planoRepositorio).deleteByVisitaNavioId(1L);
        verify(planoRepositorio).saveAll(anyList());
        verify(visitaNavioServico).registrarEvento(
                eq(visita),
                eq(null),
                eq("CRANE_PLAN_ATUALIZADO"),
                any(String.class),
                eq("planejador"),
                eq(null),
                eq("PUBLICADO"));
        assertEquals("B01", resposta.berco());
        assertEquals(StatusPlanoGuindaste.PUBLICADO, resposta.status());
        assertEquals(1, resposta.guindastes().size());
    }

    private VisitaNavio visita(LocalDateTime inicioOperacao) {
        NavioSiderurgico navio = new NavioSiderurgico();
        ReflectionTestUtils.setField(navio, "id", 7L);
        navio.setNome("Navio Teste");

        VisitaNavio visita = new VisitaNavio();
        ReflectionTestUtils.setField(visita, "id", 1L);
        visita.setNavio(navio);
        visita.setCodigoVisita("VV-001");
        visita.setBercoPrevisto("B01");
        visita.setFase(inicioOperacao == null ? FaseVisitaNavio.ATRACADA : FaseVisitaNavio.OPERANDO);
        visita.setInicioOperacao(inicioOperacao);
        return visita;
    }

    private ItemOperacaoNavio item(int quantidade, int porao, StatusItemCarga status) {
        ItemOperacaoNavio item = new ItemOperacaoNavio();
        item.setQuantidade(quantidade);
        item.setPoraoPlanejado(porao);
        item.setStatus(status);
        return item;
    }

    private PlanoGuindasteVisita plano(Long id, LocalDateTime inicio, LocalDateTime fim) {
        PlanoGuindasteVisita plano = new PlanoGuindasteVisita();
        ReflectionTestUtils.setField(plano, "id", id);
        plano.setCodigoGuindaste("QC-01");
        plano.setRecursoCais("STS-01");
        plano.setPorao(1);
        plano.setWorkQueueId(101L);
        plano.setSequencia(1);
        plano.setMovimentosPlanejados(15);
        plano.setProdutividadePlanejadaMovimentosHora(new BigDecimal("12.00"));
        plano.setInicioPlanejado(inicio);
        plano.setFimPlanejado(fim);
        plano.setStatus(StatusPlanoGuindaste.EM_EXECUCAO);
        plano.setBerco("B01");
        plano.setUsuario("planejador");
        return plano;
    }

    private AlocacaoGuindasteDTO alocacao(
            String codigo,
            int porao,
            int sequencia,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        return new AlocacaoGuindasteDTO(
                null,
                codigo,
                "STS-01",
                porao,
                101L,
                sequencia,
                10,
                new BigDecimal("12.00"),
                inicio,
                fim,
                null);
    }
}
