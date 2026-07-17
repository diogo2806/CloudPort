package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PredictiveReshuffflingServicoTest {

    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;

    @Mock
    private PosicaoPatioRepositorio posicaoRepositorio;

    @Mock
    private OrdemTrabalhoPatioServico ordemServico;

    @Mock
    private MapaOcupacaoServico mapaOcupacao;

    private PredictiveReshuffflingServico servico;
    private ConteinerPatio conteinerBase;
    private ConteinerPatio bloqueador;
    private PosicaoPatio destino;

    @BeforeEach
    void setUp() {
        servico = new PredictiveReshuffflingServico(
                conteinerRepositorio,
                posicaoRepositorio,
                ordemServico,
                mapaOcupacao);
        conteinerBase = conteiner(
                1L, "BASE001", new PosicaoPatio(10L, 1, 1, "CAMADA_1"), LocalDateTime.now().minusHours(48));
        bloqueador = conteiner(
                2L, "TOPO001", new PosicaoPatio(11L, 1, 1, "CAMADA_2"), LocalDateTime.now().minusHours(1));
        destino = new PosicaoPatio(20L, 2, 2, "CAMADA_1");
    }

    @Test
    void deveIdentificarOConteinerDaCamadaSuperiorComoBloqueador() {
        when(mapaOcupacao.obterNivelOcupacao()).thenReturn(MapaOcupacaoServico.NivelOcupacaoEnum.BAIXA);
        when(conteinerRepositorio.findAll()).thenReturn(List.of(conteinerBase, bloqueador));
        when(posicaoRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc())
                .thenReturn(List.of(conteinerBase.getPosicao(), bloqueador.getPosicao(), destino));

        PredictiveReshuffflingServico.PlanoReshuffflingDto plano = servico.analisarNecessidadeReshuffling();

        assertThat(plano.isRecomendado()).isTrue();
        assertThat(plano.getConteinersParaReshuffling()).hasSize(1);
        PredictiveReshuffflingServico.ConteinerParaReshuffflingDto candidato =
                plano.getConteinersParaReshuffling().get(0);
        assertThat(candidato.getCodigoConteiner()).isEqualTo("TOPO001");
        assertThat(candidato.getNovaPosicao().getPosicaoId()).isEqualTo(20L);
        assertThat(candidato.getChaveIdempotencia()).isEqualTo("RESHUFFLING:TOPO001:POSICAO:11");
    }

    @Test
    void deveIgnorarPosicaoComReservaAtiva() {
        PosicaoPatio destinoReservado = new PosicaoPatio(19L, 1, 2, "CAMADA_1");
        destinoReservado.reservar("OUTRA", "OUTRO001", LocalDateTime.now().plusHours(1));
        when(posicaoRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc())
                .thenReturn(List.of(destinoReservado, destino));

        List<PredictiveReshuffflingServico.ConteinerParaReshuffflingDto> candidatos =
                servico.identificarCandidatos(List.of(conteinerBase, bloqueador));

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).getNovaPosicao().getPosicaoId()).isEqualTo(20L);
    }

    @Test
    void deveCriarOrdemComDestinoRealEChaveIdempotente() {
        PredictiveReshuffflingServico.NovaPosicaoReshuffflingDto novaPosicao =
                new PredictiveReshuffflingServico.NovaPosicaoReshuffflingDto(20L, 2, 2, "CAMADA_1");
        PredictiveReshuffflingServico.ConteinerParaReshuffflingDto candidato =
                new PredictiveReshuffflingServico.ConteinerParaReshuffflingDto(
                        "TOPO001",
                        1,
                        1,
                        "CAMADA_2",
                        "BLOQUEANDO_BASE001",
                        novaPosicao,
                        "RESHUFFLING:TOPO001:POSICAO:11");
        when(conteinerRepositorio.findByCodigoIgnoreCase("TOPO001")).thenReturn(Optional.of(bloqueador));
        when(conteinerRepositorio.findAll()).thenReturn(List.of(conteinerBase, bloqueador));
        when(posicaoRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc())
                .thenReturn(List.of(destino));

        servico.executarReshuffflingConteiner(candidato);

        ArgumentCaptor<OrdemTrabalhoPatioRequisicaoDto> captor =
                ArgumentCaptor.forClass(OrdemTrabalhoPatioRequisicaoDto.class);
        verify(ordemServico).registrarOuReutilizarRemanejamento(captor.capture());
        OrdemTrabalhoPatioRequisicaoDto requisicao = captor.getValue();
        assertThat(requisicao.getTipoMovimento()).isEqualTo(TipoMovimentoPatio.REMANEJAMENTO);
        assertThat(requisicao.getLinhaDestino()).isEqualTo(2);
        assertThat(requisicao.getColunaDestino()).isEqualTo(2);
        assertThat(requisicao.getCamadaDestino()).isEqualTo("CAMADA_1");
        assertThat(requisicao.getChaveIdempotencia()).isEqualTo("RESHUFFLING:TOPO001:POSICAO:11");
    }

    private ConteinerPatio conteiner(Long id,
                                      String codigo,
                                      PosicaoPatio posicao,
                                      LocalDateTime atualizadoEm) {
        ConteinerPatio conteiner = new ConteinerPatio();
        conteiner.setId(id);
        conteiner.setCodigo(codigo);
        conteiner.setStatus(StatusConteiner.ARMAZENADO);
        conteiner.setTipoCarga(TipoCargaConteiner.SECO);
        conteiner.setDestino("BERCO_A");
        conteiner.setPosicao(posicao);
        conteiner.setAtualizadoEm(atualizadoEm);
        return conteiner;
    }
}
