package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MapaOcupacaoServicoTest {

    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;

    @Mock
    private PosicaoPatioRepositorio posicaoRepositorio;

    @InjectMocks
    private MapaOcupacaoServico mapaOcupacaoServico;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGerarHeatmapComConteineres() {
        List<ConteinerPatio> conteineres = criarConteinersTest();
        List<PosicaoPatio> posicoes = criarPosicoes();

        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        when(posicaoRepositorio.findAll()).thenReturn(posicoes);

        var heatmap = mapaOcupacaoServico.gerarHeatmap();

        assertNotNull(heatmap);
        assertTrue(heatmap.getPercentualOcupacaoGeral() > 0);
    }

    @Test
    void testObterNivelOcupacaoBaixa() {
        when(conteinerRepositorio.findAll()).thenReturn(criarConteinersTest());
        when(posicaoRepositorio.findAll()).thenReturn(criarPosicoes());

        var nivel = mapaOcupacaoServico.obterNivelOcupacao();

        assertNotNull(nivel);
        assertEquals(MapaOcupacaoServico.NivelOcupacaoEnum.BAIXA, nivel);
    }

    @Test
    void testObterDistanciaParaGate() {
        PosicaoPatio pos = new PosicaoPatio(1L, 10, 10, "CAMADA_1");
        ConteinerPatio conteiner = new ConteinerPatio(1L, "CONT001", StatusConteiner.ARMAZENADO,
                null, "DESTINO_A", pos, LocalDateTime.now());

        Integer distancia = mapaOcupacaoServico.obterDistanciaMediaParaGate(conteiner);

        assertEquals(20, distancia);
    }

    private List<ConteinerPatio> criarConteinersTest() {
        List<ConteinerPatio> conteineres = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            PosicaoPatio pos = new PosicaoPatio((long) i, i * 5, i * 5, "CAMADA_1");
            ConteinerPatio conteiner = new ConteinerPatio(
                    (long) i,
                    "CONT00" + i,
                    StatusConteiner.ARMAZENADO,
                    null,
                    "DESTINO_A",
                    pos,
                    LocalDateTime.now()
            );
            conteineres.add(conteiner);
        }

        return conteineres;
    }

    private List<PosicaoPatio> criarPosicoes() {
        List<PosicaoPatio> posicoes = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                posicoes.add(new PosicaoPatio((long) (i * 100 + j), i, j, "CAMADA_1"));
            }
        }

        return posicoes;
    }
}
