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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OtimizadorRotasPatioServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    @InjectMocks
    private OtimizadorRotasPatioServico otimizadorServico;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
    void testCalcularDistancia() {
        PosicaoPatio p1 = new PosicaoPatio(1L, 0, 0, "CAMADA_1");
        PosicaoPatio p2 = new PosicaoPatio(2L, 3, 4, "CAMADA_1");

        double distancia = otimizadorServico.calcularDistancia(p1, p2);

        assertEquals(7.0, distancia, 0.01);
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

    private List<OrdemTrabalhoPatio> criarOrdensTest() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();

        PosicaoPatio pos1 = new PosicaoPatio(1L, 0, 0, "CAMADA_1");
        ConteinerPatio conteiner1 = new ConteinerPatio(1L, "CONT001", StatusConteiner.ARMAZENADO,
                null, "DESTINO_A", pos1, LocalDateTime.now());
        OrdemTrabalhoPatio ordem1 = new OrdemTrabalhoPatio(
                conteiner1, "CONT001", "CARGA_A", "DESTINO_A",
                5, 5, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        ordens.add(ordem1);

        PosicaoPatio pos2 = new PosicaoPatio(2L, 10, 10, "CAMADA_1");
        ConteinerPatio conteiner2 = new ConteinerPatio(2L, "CONT002", StatusConteiner.ARMAZENADO,
                null, "DESTINO_B", pos2, LocalDateTime.now());
        OrdemTrabalhoPatio ordem2 = new OrdemTrabalhoPatio(
                conteiner2, "CONT002", "CARGA_B", "DESTINO_B",
                15, 15, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        ordens.add(ordem2);

        PosicaoPatio pos3 = new PosicaoPatio(3L, 6, 6, "CAMADA_1");
        ConteinerPatio conteiner3 = new ConteinerPatio(3L, "CONT003", StatusConteiner.ARMAZENADO,
                null, "DESTINO_C", pos3, LocalDateTime.now());
        OrdemTrabalhoPatio ordem3 = new OrdemTrabalhoPatio(
                conteiner3, "CONT003", "CARGA_C", "DESTINO_C",
                8, 8, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        ordens.add(ordem3);

        return ordens;
    }
}
