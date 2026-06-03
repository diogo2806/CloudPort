package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OtimizadorDualCyclingServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    @InjectMocks
    private OtimizadorDualCyclingServico otimizadorDualCycling;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        assertTrue(pairs.stream().allMatch(pair -> pair.getDistanciaRetorno() >= 0));
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
        PosicaoPatio pos1 = new PosicaoPatio(1L, 10, 10, "CAMADA_1");
        ConteinerPatio cont1 = new ConteinerPatio(1L, "CONT001", StatusConteiner.ARMAZENADO,
                null, "DESTINO_A", pos1, LocalDateTime.now());
        OrdemTrabalhoPatio ordem1 = new OrdemTrabalhoPatio(
                cont1, "CONT001", "CARGA_A", "DESTINO_A",
                10, 10, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        PosicaoPatio pos2 = new PosicaoPatio(2L, 20, 20, "CAMADA_1");
        ConteinerPatio cont2 = new ConteinerPatio(2L, "CONT002", StatusConteiner.ARMAZENADO,
                null, "DESTINO_A", pos2, LocalDateTime.now());
        OrdemTrabalhoPatio ordem2 = new OrdemTrabalhoPatio(
                cont2, "CONT002", "CARGA_A", "DESTINO_A",
                20, 20, "CAMADA_1",
                TipoMovimentoPatio.REMOCAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<OrdemTrabalhoPatio> ordens = List.of(ordem1, ordem2);
        when(ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE))
                .thenReturn(ordens);

        var pairs = otimizadorDualCycling.gerarPairs(20);

        assertNotNull(pairs);
    }

    private List<OrdemTrabalhoPatio> criarOrdensComEntradaESaidaTest() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();

        PosicaoPatio pos1 = new PosicaoPatio(1L, 5, 5, "CAMADA_1");
        ConteinerPatio cont1 = new ConteinerPatio(1L, "CONT001", StatusConteiner.ARMAZENADO,
                null, "DESTINO_A", pos1, LocalDateTime.now());
        OrdemTrabalhoPatio ordem1 = new OrdemTrabalhoPatio(
                cont1, "CONT001", "CARGA_A", "DESTINO_A",
                10, 10, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        definirId(ordem1, 1L);
        ordens.add(ordem1);

        PosicaoPatio pos2 = new PosicaoPatio(2L, 15, 15, "CAMADA_1");
        ConteinerPatio cont2 = new ConteinerPatio(2L, "CONT002", StatusConteiner.ARMAZENADO,
                null, "DESTINO_A", pos2, LocalDateTime.now());
        OrdemTrabalhoPatio ordem2 = new OrdemTrabalhoPatio(
                cont2, "CONT002", "CARGA_A", "DESTINO_A",
                12, 12, "CAMADA_1",
                TipoMovimentoPatio.REMOCAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        definirId(ordem2, 2L);
        ordens.add(ordem2);

        PosicaoPatio pos3 = new PosicaoPatio(3L, 8, 8, "CAMADA_1");
        ConteinerPatio cont3 = new ConteinerPatio(3L, "CONT003", StatusConteiner.ARMAZENADO,
                null, "DESTINO_B", pos3, LocalDateTime.now());
        OrdemTrabalhoPatio ordem3 = new OrdemTrabalhoPatio(
                cont3, "CONT003", "CARGA_B", "DESTINO_B",
                20, 20, "CAMADA_1",
                TipoMovimentoPatio.ALOCACAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        definirId(ordem3, 3L);
        ordens.add(ordem3);

        PosicaoPatio pos4 = new PosicaoPatio(4L, 25, 25, "CAMADA_1");
        ConteinerPatio cont4 = new ConteinerPatio(4L, "CONT004", StatusConteiner.ARMAZENADO,
                null, "DESTINO_B", pos4, LocalDateTime.now());
        OrdemTrabalhoPatio ordem4 = new OrdemTrabalhoPatio(
                cont4, "CONT004", "CARGA_B", "DESTINO_B",
                22, 22, "CAMADA_1",
                TipoMovimentoPatio.REMOCAO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        definirId(ordem4, 4L);
        ordens.add(ordem4);

        return ordens;
    }

    private List<OrdemTrabalhoPatio> criarOrdensEmBlocosAdjacentesTest() {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            PosicaoPatio pos = new PosicaoPatio((long) i, i * 5, i * 5, "CAMADA_1");
            ConteinerPatio cont = new ConteinerPatio((long) i, "CONT00" + i, StatusConteiner.ARMAZENADO,
                    null, "DESTINO_A", pos, LocalDateTime.now());
            OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                    cont, "CONT00" + i, "CARGA_A", "DESTINO_A",
                    i * 8, i * 8, "CAMADA_1",
                    TipoMovimentoPatio.ALOCACAO,
                    StatusOrdemTrabalhoPatio.PENDENTE,
                    StatusConteiner.ARMAZENADO,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            definirId(ordem, (long) i + 1);
            ordens.add(ordem);
        }

        for (int i = 3; i < 6; i++) {
            PosicaoPatio pos = new PosicaoPatio((long) i, i * 5, i * 5, "CAMADA_1");
            ConteinerPatio cont = new ConteinerPatio((long) i, "CONT00" + i, StatusConteiner.ARMAZENADO,
                    null, "DESTINO_A", pos, LocalDateTime.now());
            OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                    cont, "CONT00" + i, "CARGA_A", "DESTINO_A",
                    i * 8 + 2, i * 8 + 2, "CAMADA_1",
                    TipoMovimentoPatio.REMOCAO,
                    StatusOrdemTrabalhoPatio.PENDENTE,
                    StatusConteiner.ARMAZENADO,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            definirId(ordem, (long) i + 1);
            ordens.add(ordem);
        }

        return ordens;
    }

    private void definirId(OrdemTrabalhoPatio ordem, Long id) {
        try {
            Field field = OrdemTrabalhoPatio.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ordem, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
