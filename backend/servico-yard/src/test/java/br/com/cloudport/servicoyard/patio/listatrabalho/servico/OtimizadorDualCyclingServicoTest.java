package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
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

public class OtimizadorDualCyclingServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    private OtimizadorDualCyclingServico otimizadorDualCycling;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        otimizadorDualCycling = new OtimizadorDualCyclingServico(ordemRepositorio, new YardDualCycleService());
    }

    @Test
    void testAnalisarPairingsPotenciais() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensComEntradaESaidaTest();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        var analise = otimizadorDualCycling.analisarPairingsPotenciais();

        assertNotNull(analise);
        assertTrue(analise.getPercentualEconomia() >= 0);
        assertTrue(analise.getEconomiaKm() >= 0);
    }

    @Test
    void testGerarPairsOtimizados() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensComEntradaESaidaTest();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        var pairs = otimizadorDualCycling.gerarPairs(10);

        assertNotNull(pairs);
        assertFalse(pairs.isEmpty());
    }

    @Test
    void testGerarPairComBlocoAdjacente() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensEmBlocosAdjacentesTest();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        var pairs = otimizadorDualCycling.gerarPairs(15);

        assertNotNull(pairs);
        assertTrue(pairs.stream()
                .anyMatch(p -> p.getDistanciaRetorno() < 30));
    }

    @Test
    void testObterSequenciaOtimizadaComDualCycling() {
        List<OrdemTrabalhoPatio> ordens = criarOrdensComEntradaESaidaTest();
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        var sequencia = otimizadorDualCycling.obterSequenciaOtimizadaComDualCycling();

        assertNotNull(sequencia);
        assertEquals(4, sequencia.size());
    }

    @Test
    void testCalcularDistanciaRetorno() {
        OrdemTrabalhoPatio ordem1 = new OrdemTrabalhoPatio(
                criarConteiner(1L, "CONT001", "DESTINO_A", 10, 10),
                "CONT001", "CARGA_A", "DESTINO_A", 10, 10, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO, StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO, LocalDateTime.now(), LocalDateTime.now());
        ordem1.setId(1L);

        OrdemTrabalhoPatio ordem2 = new OrdemTrabalhoPatio(
                criarConteiner(2L, "CONT002", "DESTINO_A", 20, 20),
                "CONT002", "CARGA_A", "DESTINO_A", 20, 20, "CAMADA_1",
                TipoMovimentoPatio.REMOCAO, StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO, LocalDateTime.now(), LocalDateTime.now());
        ordem2.setId(2L);

        List<OrdemTrabalhoPatio> ordens = List.of(ordem1, ordem2);
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        var pairs = otimizadorDualCycling.gerarPairs(20);

        assertNotNull(pairs);
    }

    private ConteinerPatio criarConteiner(Long id, String codigo, String destino, int linha, int coluna) {
        PosicaoPatio pos = new PosicaoPatio(id, linha, coluna, "CAMADA_1");
        ConteinerPatio cont = new ConteinerPatio();
        cont.setId(id);
        cont.setCodigo(codigo);
        cont.setStatus(StatusConteiner.ARMAZENADO);
        cont.setDestino(destino);
        cont.setPosicao(pos);
        return cont;
    }

    private List<OrdemTrabalhoPatio> criarOrdensComEntradaESaidaTest() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();

        OrdemTrabalhoPatio o1 = new OrdemTrabalhoPatio(
                criarConteiner(1L, "CONT001", "DESTINO_A", 5, 5), "CONT001", "CARGA_A", "DESTINO_A",
                10, 10, "CAMADA_1", TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE, StatusConteiner.ARMAZENADO,
                LocalDateTime.now(), LocalDateTime.now());
        o1.setId(1L);
        ordens.add(o1);

        OrdemTrabalhoPatio o2 = new OrdemTrabalhoPatio(
                criarConteiner(2L, "CONT002", "DESTINO_A", 15, 15), "CONT002", "CARGA_A", "DESTINO_A",
                12, 12, "CAMADA_1", TipoMovimentoPatio.REMOCAO,
                StatusOrdemTrabalhoPatio.PENDENTE, StatusConteiner.ARMAZENADO,
                LocalDateTime.now(), LocalDateTime.now());
        o2.setId(2L);
        ordens.add(o2);

        OrdemTrabalhoPatio o3 = new OrdemTrabalhoPatio(
                criarConteiner(3L, "CONT003", "DESTINO_B", 8, 8), "CONT003", "CARGA_B", "DESTINO_B",
                20, 20, "CAMADA_1", TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE, StatusConteiner.ARMAZENADO,
                LocalDateTime.now(), LocalDateTime.now());
        o3.setId(3L);
        ordens.add(o3);

        OrdemTrabalhoPatio o4 = new OrdemTrabalhoPatio(
                criarConteiner(4L, "CONT004", "DESTINO_B", 25, 25), "CONT004", "CARGA_B", "DESTINO_B",
                22, 22, "CAMADA_1", TipoMovimentoPatio.REMOCAO,
                StatusOrdemTrabalhoPatio.PENDENTE, StatusConteiner.ARMAZENADO,
                LocalDateTime.now(), LocalDateTime.now());
        o4.setId(4L);
        ordens.add(o4);

        return ordens;
    }

    private List<OrdemTrabalhoPatio> criarOrdensEmBlocosAdjacentesTest() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            OrdemTrabalhoPatio o = new OrdemTrabalhoPatio(
                    criarConteiner((long) i, "CONT00" + i, "DESTINO_A", i * 5, i * 5),
                    "CONT00" + i, "CARGA_A", "DESTINO_A",
                    i * 2, i * 2, "CAMADA_1", TipoMovimentoPatio.ALOCACAO,
                    StatusOrdemTrabalhoPatio.PENDENTE, StatusConteiner.ARMAZENADO,
                    LocalDateTime.now(), LocalDateTime.now());
            o.setId((long) i + 1);
            ordens.add(o);
        }

        for (int i = 3; i < 6; i++) {
            OrdemTrabalhoPatio o = new OrdemTrabalhoPatio(
                    criarConteiner((long) i, "CONT00" + i, "DESTINO_A", i * 5, i * 5),
                    "CONT00" + i, "CARGA_A", "DESTINO_A",
                    i * 2 + 5, i * 2 + 5, "CAMADA_1", TipoMovimentoPatio.REMOCAO,
                    StatusOrdemTrabalhoPatio.PENDENTE, StatusConteiner.ARMAZENADO,
                    LocalDateTime.now(), LocalDateTime.now());
            o.setId((long) i + 1);
            ordens.add(o);
        }

        return ordens;
    }
}
