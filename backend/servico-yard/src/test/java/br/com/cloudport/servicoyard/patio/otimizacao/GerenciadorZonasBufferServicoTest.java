package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class GerenciadorZonasBufferServicoTest {

    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;

    @Mock
    private PosicaoPatioRepositorio posicaoRepositorio;

    @InjectMocks
    private GerenciadorZonasBufferServico gerenciadorBuffer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testObterConfiguracaoBuffer() {
        List<ConteinerPatio> conteineres = criarConteinersTest();
        List<PosicaoPatio> posicoes = criarPosicoes();

        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        when(posicaoRepositorio.findAll()).thenReturn(posicoes);

        var config = gerenciadorBuffer.obterConfiguracaoBuffer();

        assertNotNull(config);
        assertTrue(config.getTotalZonasReservadas() > 0);
        assertFalse(config.getCorredoresLivres().isEmpty());
    }

    @Test
    void testPodeAlocarEmZona() {
        // Zona livre
        boolean pode1 = gerenciadorBuffer.podeAlocarEmZona(50, 50);
        assertTrue(pode1);

        // Zona reservada (buffer)
        gerenciadorBuffer.reservarZonaBuffer(5, 5, "Teste");
        boolean pode2 = gerenciadorBuffer.podeAlocarEmZona(5, 5);
        assertFalse(pode2);
    }

    @Test
    void testReservarELiberarZonaBuffer() {
        gerenciadorBuffer.reservarZonaBuffer(10, 10, "Manobra");
        assertFalse(gerenciadorBuffer.podeAlocarEmZona(10, 10));

        gerenciadorBuffer.liberarZonaBuffer(10, 10);
        assertTrue(gerenciadorBuffer.podeAlocarEmZona(10, 10));
    }

    @Test
    void testAnalisarCorredoresManobra() {
        List<ConteinerPatio> conteineres = criarConteinersTest();
        List<PosicaoPatio> posicoes = criarPosicoes();

        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        when(posicaoRepositorio.findAll()).thenReturn(posicoes);

        var analise = gerenciadorBuffer.analisarCorredoresManobra();

        assertNotNull(analise);
        assertTrue(analise.getLarguraMediaCorredor() >= 0);
    }

    @Test
    void testIdentificarZonasEmRisco() {
        List<ConteinerPatio> conteineres = criarConteinersTest();
        List<PosicaoPatio> posicoes = criarPosicoes();

        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        when(posicaoRepositorio.findAll()).thenReturn(posicoes);

        var alertas = gerenciadorBuffer.identificarZonasEmRisco();

        assertNotNull(alertas);
    }

    private List<ConteinerPatio> criarConteinersTest() {
        List<ConteinerPatio> conteineres = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            PosicaoPatio pos = new PosicaoPatio((long) i, i * 10, i * 10, "CAMADA_1");
            ConteinerPatio conteiner = new ConteinerPatio((long) i, "CONT00" + i, StatusConteiner.ARMAZENADO,
                    null, "DESTINO_A", pos, LocalDateTime.now());
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
