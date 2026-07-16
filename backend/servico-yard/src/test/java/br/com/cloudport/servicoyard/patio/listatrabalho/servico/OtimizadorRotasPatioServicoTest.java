package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EstatisticasOtimizacaoRotaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OtimizadorRotasPatioServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    private OtimizadorRotasPatioServico otimizadorServico;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        otimizadorServico = new OtimizadorRotasPatioServico(ordemRepositorio, new YardDualCycleService());
    }

    @Test
    void deveOtimizarRotaComOrdensPendentes() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensTeste();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        List<OrdemTrabalhoPatio> resultado = otimizadorServico.otimizarRota();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremOrdens() {
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(new ArrayList<>());

        List<OrdemTrabalhoPatio> resultado = otimizadorServico.otimizarRota();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveCalcularDistanciaTotalFinita() {
        double distanciaTotal = otimizadorServico.calcularDistanciaTotal(criarOrdensTeste());

        assertTrue(Double.isFinite(distanciaTotal));
        assertTrue(distanciaTotal >= 0);
    }

    @Test
    void devePartirDoDestinoDaPrimeiraOrdemAoEscolherAProxima() {
        OrdemTrabalhoPatio primeira = criarOrdem(1L, "CONT001", 0, 0, 100, 100);
        OrdemTrabalhoPatio proximaDaOrigem = criarOrdem(2L, "CONT002", 1, 1, 2, 2);
        OrdemTrabalhoPatio proximaDoDestino = criarOrdem(3L, "CONT003", 99, 99, 98, 98);
        List<OrdemTrabalhoPatio> ordens = List.of(primeira, proximaDaOrigem, proximaDoDestino);
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        List<OrdemTrabalhoPatio> resultado = otimizadorServico.otimizarRota();

        assertEquals(List.of(1L, 3L, 2L), resultado.stream().map(OrdemTrabalhoPatio::getId).toList());
    }

    @Test
    void deveManterPercentualZeroQuandoDistanciaOriginalForZero() {
        OrdemTrabalhoPatio ordem = criarOrdem(1L, "CONT001", 0, 0, 0, 0);

        EstatisticasOtimizacaoRotaDto estatisticas = otimizadorServico
                .obterEstatisticasOtimizacao(List.of(ordem), List.of(ordem));

        assertEquals(0.0, estatisticas.getPercentualMelhoria());
        assertTrue(Double.isFinite(estatisticas.getPercentualMelhoria()));
    }

    @Test
    void deveRetornarEstatisticasTipadas() {
        List<OrdemTrabalhoPatio> ordensOriginais = criarOrdensTeste();
        List<OrdemTrabalhoPatio> ordensOtimizadas = new ArrayList<>(ordensOriginais);

        EstatisticasOtimizacaoRotaDto estatisticas = otimizadorServico
                .obterEstatisticasOtimizacao(ordensOriginais, ordensOtimizadas);

        assertNotNull(estatisticas);
        assertEquals(3, estatisticas.getTotalOrdens());
        assertEquals(0.0, estatisticas.getPercentualMelhoria());
        assertEquals(estatisticas.getPercentualMelhoria(), estatisticas.getPercentualMejora());
    }

    private ConteinerPatio criarConteiner(Long id, String codigo, String destino, PosicaoPatio posicao) {
        ConteinerPatio conteiner = new ConteinerPatio();
        conteiner.setId(id);
        conteiner.setCodigo(codigo);
        conteiner.setStatus(StatusConteiner.ARMAZENADO);
        conteiner.setDestino(destino);
        conteiner.setPosicao(posicao);
        return conteiner;
    }

    private OrdemTrabalhoPatio criarOrdem(Long id,
                                           String codigo,
                                           int linhaAtual,
                                           int colunaAtual,
                                           int linhaDestino,
                                           int colunaDestino) {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                criarConteiner(id, codigo, "DESTINO", new PosicaoPatio(id, linhaAtual, colunaAtual, "CAMADA_1")),
                codigo,
                "CARGA",
                "DESTINO",
                linhaDestino,
                colunaDestino,
                "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now());
        ordem.setId(id);
        return ordem;
    }

    private List<OrdemTrabalhoPatio> criarOrdensTeste() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();
        ordens.add(criarOrdem(1L, "CONT001", 0, 0, 5, 5));
        ordens.add(criarOrdem(2L, "CONT002", 10, 10, 15, 15));
        ordens.add(criarOrdem(3L, "CONT003", 6, 6, 8, 8));
        return ordens;
    }
}
