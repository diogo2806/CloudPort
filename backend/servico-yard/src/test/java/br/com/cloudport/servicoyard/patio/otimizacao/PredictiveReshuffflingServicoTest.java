package br.com.cloudport.servicoyard.patio.otimizacao;

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
    private MapaOcupacaoServico mapaOcupacao;

    @InjectMocks
    private PredictiveReshuffflingServico predictiveReshuffling;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAnalisarNecessidadeReshuffflingComBaixaOcupacao() {
        when(mapaOcupacao.obterNivelOcupacao())
                .thenReturn(MapaOcupacaoServico.NivelOcupacaoEnum.BAIXA);

        List<ConteinerPatio> conteineres = criarConteinersTest();
        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        when(ordemRepositorio.findAll()).thenReturn(criarOrdensSobreConteineres(conteineres));

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
        when(conteinerRepositorio.findAll()).thenReturn(conteineres);
        when(ordemRepositorio.findAll()).thenReturn(criarOrdensSobreConteineres(conteineres));

        var candidatos = predictiveReshuffling.identificarCandidatos(conteineres);

        assertNotNull(candidatos);
        assertTrue(candidatos.size() > 0);
    }

    private List<ConteinerPatio> criarConteinersTest() {
        List<ConteinerPatio> conteineres = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            PosicaoPatio pos = new PosicaoPatio((long) i, i * 5, i * 5, "CAMADA_1");
            ConteinerPatio c = new ConteinerPatio();
            c.setId((long) i);
            c.setCodigo("CONT00" + i);
            c.setStatus(StatusConteiner.ARMAZENADO);
            c.setDestino("DESTINO_A");
            c.setPosicao(pos);
            c.setAtualizadoEm(LocalDateTime.now().minusHours(48));
            conteineres.add(c);
        }
        return conteineres;
    }

    private List<OrdemTrabalhoPatio> criarOrdensSobreConteineres(List<ConteinerPatio> conteineres) {
        List<OrdemTrabalhoPatio> ordens = new ArrayList<>();
        for (ConteinerPatio base : conteineres) {
            ConteinerPatio conteinerEmCima = new ConteinerPatio();
            conteinerEmCima.setId(base.getId() + 100);
            conteinerEmCima.setCodigo("TOP_" + base.getCodigo());
            conteinerEmCima.setStatus(StatusConteiner.ARMAZENADO);
            conteinerEmCima.setDestino("DESTINO_B");
            conteinerEmCima.setPosicao(base.getPosicao());

            LocalDateTime agora = LocalDateTime.now();
            OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                    conteinerEmCima, "TOP_" + base.getCodigo(), "SECO", "DESTINO_B",
                    base.getPosicao().getLinha() + 5,
                    base.getPosicao().getColuna() + 5,
                    "CAMADA_1",
                    TipoMovimentoPatio.REMANEJAMENTO,
                    StatusOrdemTrabalhoPatio.PENDENTE,
                    StatusConteiner.ARMAZENADO,
                    agora.minusHours(1),
                    agora.minusHours(1)
            );
            ordens.add(ordem);
        }
        return ordens;
    }
}
