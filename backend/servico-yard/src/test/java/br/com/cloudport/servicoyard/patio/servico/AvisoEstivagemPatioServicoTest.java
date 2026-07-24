package br.com.cloudport.servicoyard.patio.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.dto.ViolacaoEstivagemPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EstadoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.HistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoRegraEstivagemPatio;
import br.com.cloudport.servicoyard.patio.repositorio.AvisoEstivagemPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.HistoricoAvisoEstivagemPatioRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AvisoEstivagemPatioServicoTest {

    @Mock
    private AvisoEstivagemPatioRepositorio avisoRepositorio;

    @Mock
    private HistoricoAvisoEstivagemPatioRepositorio historicoRepositorio;

    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;

    @Mock
    private DetectorAvisoEstivagemPatioServico detectorServico;

    @InjectMocks
    private AvisoEstivagemPatioServico servico;

    private ConteinerPatio unidade;
    private ViolacaoEstivagemPatioDto violacao;

    @BeforeEach
    void preparar() {
        PosicaoPatio posicao = new PosicaoPatio(11L, 3, 7, "2");
        posicao.setBloco("A");
        posicao.setAreaPermitida(true);
        unidade = new ConteinerPatio();
        unidade.setId(22L);
        unidade.setCodigo("MSCU1234567");
        unidade.setPosicao(posicao);
        violacao = new ViolacaoEstivagemPatioDto(
                TipoRegraEstivagemPatio.PESO,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Peso observado: 30 t",
                "Peso máximo: 20 t",
                "Mover para posição compatível",
                true);
        when(avisoRepositorio.save(any(AvisoEstivagemPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(historicoRepositorio.save(any(HistoricoAvisoEstivagemPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveCriarUmUnicoCasoParaAChaveEstavel() {
        when(conteinerRepositorio.findAllByOrderByCodigoAsc()).thenReturn(List.of(unidade));
        when(detectorServico.detectar(unidade, List.of(unidade))).thenReturn(List.of(violacao));
        when(avisoRepositorio.findComBloqueioByChaveEstavel(anyString())).thenReturn(Optional.empty());
        when(avisoRepositorio.findByCodigoUnidadeIgnoreCaseAndEstadoIn(anyString(), anyCollection()))
                .thenReturn(List.of());
        when(avisoRepositorio.findAllByEstadoInOrderByAtualizadoEmDesc(anyCollection()))
                .thenReturn(List.of());

        servico.sincronizarInventario("inventario-job");

        ArgumentCaptor<AvisoEstivagemPatio> captor = ArgumentCaptor.forClass(AvisoEstivagemPatio.class);
        verify(avisoRepositorio).save(captor.capture());
        AvisoEstivagemPatio criado = captor.getValue();
        assertThat(criado.getChaveEstavel()).isEqualTo("MSCU1234567|3/7/2|PESO");
        assertThat(criado.getEstado()).isEqualTo(EstadoAvisoEstivagemPatio.ABERTO);
        assertThat(criado.getOcorrencias()).isEqualTo(1);
        verify(historicoRepositorio).save(any(HistoricoAvisoEstivagemPatio.class));
    }

    @Test
    void naoDeveDuplicarCasoAtivoDaMesmaUnidadePosicaoERegra() {
        AvisoEstivagemPatio existente = aviso(EstadoAvisoEstivagemPatio.ABERTO, 1);
        when(conteinerRepositorio.findAllByOrderByCodigoAsc()).thenReturn(List.of(unidade));
        when(detectorServico.detectar(unidade, List.of(unidade))).thenReturn(List.of(violacao));
        when(avisoRepositorio.findComBloqueioByChaveEstavel(existente.getChaveEstavel()))
                .thenReturn(Optional.of(existente));
        when(avisoRepositorio.findByCodigoUnidadeIgnoreCaseAndEstadoIn(anyString(), anyCollection()))
                .thenReturn(List.of(existente));
        when(avisoRepositorio.findAllByEstadoInOrderByAtualizadoEmDesc(anyCollection()))
                .thenReturn(List.of(existente));

        servico.sincronizarInventario("inventario-job");

        assertThat(existente.getEstado()).isEqualTo(EstadoAvisoEstivagemPatio.ABERTO);
        assertThat(existente.getOcorrencias()).isEqualTo(1);
        verify(avisoRepositorio, atLeastOnce()).save(existente);
        verify(historicoRepositorio, never()).save(any(HistoricoAvisoEstivagemPatio.class));
    }

    @Test
    void deveResolverSomenteQuandoAViolacaoDeixarDeExistir() {
        AvisoEstivagemPatio existente = aviso(EstadoAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO, 1);
        when(conteinerRepositorio.findAllByOrderByCodigoAsc()).thenReturn(List.of(unidade));
        when(detectorServico.detectar(unidade, List.of(unidade))).thenReturn(List.of());
        when(avisoRepositorio.findByCodigoUnidadeIgnoreCaseAndEstadoIn(anyString(), anyCollection()))
                .thenReturn(List.of(existente));
        when(avisoRepositorio.findAllByEstadoInOrderByAtualizadoEmDesc(anyCollection()))
                .thenReturn(List.of());

        servico.sincronizarInventario("revalidador");

        assertThat(existente.getEstado()).isEqualTo(EstadoAvisoEstivagemPatio.RESOLVIDO);
        assertThat(existente.getResolvidoEm()).isNotNull();
        assertThat(existente.getResultadoRevalidacao()).contains("deixou de existir");
        verify(historicoRepositorio).save(any(HistoricoAvisoEstivagemPatio.class));
    }

    @Test
    void deveReabrirOMesmoCasoQuandoAViolacaoVoltar() {
        AvisoEstivagemPatio existente = aviso(EstadoAvisoEstivagemPatio.RESOLVIDO, 1);
        when(conteinerRepositorio.findAllByOrderByCodigoAsc()).thenReturn(List.of(unidade));
        when(detectorServico.detectar(unidade, List.of(unidade))).thenReturn(List.of(violacao));
        when(avisoRepositorio.findComBloqueioByChaveEstavel(existente.getChaveEstavel()))
                .thenReturn(Optional.of(existente));
        when(avisoRepositorio.findByCodigoUnidadeIgnoreCaseAndEstadoIn(anyString(), anyCollection()))
                .thenReturn(List.of(existente));
        when(avisoRepositorio.findAllByEstadoInOrderByAtualizadoEmDesc(anyCollection()))
                .thenReturn(List.of(existente));

        servico.sincronizarInventario("inventario-job");

        assertThat(existente.getEstado()).isEqualTo(EstadoAvisoEstivagemPatio.REABERTO);
        assertThat(existente.getOcorrencias()).isEqualTo(2);
        verify(historicoRepositorio).save(any(HistoricoAvisoEstivagemPatio.class));
    }

    @Test
    void deveBloquearDispatchOuPlanejamentoComAvisoCriticoAtivo() {
        when(avisoRepositorio.existsByCodigoPosicaoInAndSeveridadeAndEstadoIn(
                anyCollection(), any(SeveridadeAvisoEstivagemPatio.class), anyCollection()))
                .thenReturn(true);

        ResponseStatusException excecao = assertThrows(
                ResponseStatusException.class,
                () -> servico.validarOperacaoSemAvisoCritico(null, "3/7/2"));

        assertThat(excecao.getRawStatusCode()).isEqualTo(409);
        assertThat(excecao.getReason()).contains("aviso crítico");
    }

    private AvisoEstivagemPatio aviso(EstadoAvisoEstivagemPatio estado, int ocorrencias) {
        AvisoEstivagemPatio aviso = new AvisoEstivagemPatio();
        aviso.setChaveEstavel("MSCU1234567|3/7/2|PESO");
        aviso.setUnidade(unidade);
        aviso.setPosicao(unidade.getPosicao());
        aviso.setCodigoUnidade(unidade.getCodigo());
        aviso.setCodigoPosicao("3/7/2");
        aviso.setBloco("A");
        aviso.setLinha(3);
        aviso.setColuna(7);
        aviso.setCamada("2");
        aviso.setRegra(TipoRegraEstivagemPatio.PESO);
        aviso.setSeveridade(SeveridadeAvisoEstivagemPatio.CRITICA);
        aviso.setEstado(estado);
        aviso.setValorObservado(violacao.valorObservado());
        aviso.setValorEsperado(violacao.valorEsperado());
        aviso.setAcaoSugerida(violacao.acaoSugerida());
        aviso.setBloqueiaOperacao(true);
        aviso.setOcorrencias(ocorrencias);
        return aviso;
    }
}
