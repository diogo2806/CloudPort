package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PredictiveReshuffflingServicoTest {

    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    @Mock
    private OrdemTrabalhoPatioServico ordemTrabalhoPatioServico;

    @Mock
    private MapaOcupacaoServico mapaOcupacao;

    @InjectMocks
    private PredictiveReshuffflingServico predictiveReshuffling;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(ordemRepositorio.findAll()).thenReturn(new ArrayList<>());
    }

    @Test
    void testAnalisarNecessidadeReshuffflingComBaixaOcupacao() {
        when(mapaOcupacao.obterNivelOcupacao())
                .thenReturn(MapaOcupacaoServico.NivelOcupacaoEnum.BAIXA);

        List<ConteinerPatio> conteineres = criarConteinersTest();
        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        var plano = predictiveReshuffling.analisarNecessidadeReshuffling();

        assertNotNull(plano);
        assertFalse(plano.getConteinersParaReshuffling().isEmpty());
    }

    @Test
    void testAnalisarNecessidadeReshuffflingComAltaOcupacao() {
        when(mapaOcupacao.obterNivelOcupacao())
                .thenReturn(MapaOcupacaoServico.NivelOcupacaoEnum.ALTA);

        var plano = predictiveReshuffling.analisarNecessidadeReshuffling();

        assertNotNull(plano);
        assertFalse(plano.isRecomendado());
    }

    @Test
    void testIdentificarCandidatosReshufffling() {
        List<ConteinerPatio> conteineres = criarConteinersTest();

        var candidatos = predictiveReshuffling.identificarCandidatos(conteineres);

        assertNotNull(candidatos);
        assertEquals(3, candidatos.size());
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
                    LocalDateTime.now().minusHours(48)
            );
            conteineres.add(conteiner);
        }

        return conteineres;
    }
}
