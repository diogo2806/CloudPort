package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OtimizadorRotasPatioServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    private OtimizadorRotasPatioServico otimizadorServico;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        otimizadorServico = new OtimizadorRotasPatioServico(ordemRepositorio, new YardDualCycleService());
    }

    @Test
    void testOtimizarRotaComOrdensPendentes() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensTest();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        List<OrdemTrabalhoPatio> resultado = otimizadorServico.otimizarRota();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
    }

    @Test
    void testOtimizarRotaComListaVazia() {
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(new ArrayList<>());

        List<OrdemTrabalhoPatio> resultado = otimizadorServico.otimizarRota();

        assertNotNull(resultado);
        assertEquals(0, resultado.size());
    }

    @Test
    void testCalcularDistanciaTotal() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensTest();

        double distanciaTotal = otimizadorServico.calcularDistanciaTotal(ordens);

        assertTrue(distanciaTotal >= 0);
    }

    @Test
    void testOtimizarRotaComProximidade() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensTest();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        List<OrdemTrabalhoPatio> resultado = otimizadorServico.otimizarRotaComProximidade();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
    }

    @Test
    void testObtenerEstatisticasOtimizacao() {
        List<OrdemTrabalhoPatio> ordensOriginais = criarOrdensTest();
        List<OrdemTrabalhoPatio> ordensOtimizadas = new ArrayList<>(ordensOriginais);

        var stats = otimizadorServico.obterEstatisticasOtimizacao(ordensOriginais, ordensOtimizadas);

        assertNotNull(stats);
        assertEquals(3, stats.get("totalOrdens"));
        assertTrue((Double) stats.get("percentualMejora") >= 0);
    }

    private ConteinerPatio criarConteiner(Long id, String codigo, String destino, PosicaoPatio posicao) {
        ConteinerPatio c = new ConteinerPatio();
        c.setId(id);
        c.setCodigo(codigo);
        c.setStatus(StatusConteiner.ARMAZENADO);
        c.setDestino(destino);
        c.setPosicao(posicao);
        return c;
    }

    private List<OrdemTrabalhoPatio> criarOrdensTest() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();

        OrdemTrabalhoPatio ordem1 = new OrdemTrabalhoPatio(
                criarConteiner(1L, "CONT001", "DESTINO_A", new PosicaoPatio(1L, 0, 0, "CAMADA_1")),
                "CONT001", "CARGA_A", "DESTINO_A", 5, 5, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO, StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO, LocalDateTime.now(), LocalDateTime.now());
        ordem1.setId(1L);
        ordens.add(ordem1);

        OrdemTrabalhoPatio ordem2 = new OrdemTrabalhoPatio(
                criarConteiner(2L, "CONT002", "DESTINO_B", new PosicaoPatio(2L, 10, 10, "CAMADA_1")),
                "CONT002", "CARGA_B", "DESTINO_B", 15, 15, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO, StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO, LocalDateTime.now(), LocalDateTime.now());
        ordem2.setId(2L);
        ordens.add(ordem2);

        OrdemTrabalhoPatio ordem3 = new OrdemTrabalhoPatio(
                criarConteiner(3L, "CONT003", "DESTINO_C", new PosicaoPatio(3L, 6, 6, "CAMADA_1")),
                "CONT003", "CARGA_C", "DESTINO_C", 8, 8, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO, StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO, LocalDateTime.now(), LocalDateTime.now());
        ordem3.setId(3L);
        ordens.add(ordem3);

        return ordens;
    }
}
