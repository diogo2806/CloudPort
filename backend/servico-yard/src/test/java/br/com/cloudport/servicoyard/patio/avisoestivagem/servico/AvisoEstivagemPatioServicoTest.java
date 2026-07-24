package br.com.cloudport.servicoyard.patio.avisoestivagem.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoRegraEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.HistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.repositorio.AvisoEstivagemPatioRepositorio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.repositorio.HistoricoAvisoEstivagemPatioRepositorio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.servico.DetectorViolacaoEstivagemPatioServico.ViolacaoDetectada;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvisoEstivagemPatioServicoTest {

    @Mock
    private AvisoEstivagemPatioRepositorio avisoRepositorio;

    @Mock
    private HistoricoAvisoEstivagemPatioRepositorio historicoRepositorio;

    @Mock
    private DetectorViolacaoEstivagemPatioServico detector;

    private AvisoEstivagemPatioServico servico;

    @BeforeEach
    void configurar() {
        servico = new AvisoEstivagemPatioServico(avisoRepositorio, historicoRepositorio, detector);
        when(avisoRepositorio.save(any(AvisoEstivagemPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(avisoRepositorio.saveAndFlush(any(AvisoEstivagemPatio.class)))
                .thenAnswer(invocacao -> {
                    AvisoEstivagemPatio aviso = invocacao.getArgument(0);
                    aviso.setId(10L);
                    return aviso;
                });
        when(historicoRepositorio.save(any(HistoricoAvisoEstivagemPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveAbrirUmaUnicaOcorrenciaParaMesmaChaveEstavel() {
        ViolacaoDetectada violacao = violacao();
        AvisoEstivagemPatio existente = avisoAtivo();
        when(detector.detectar()).thenReturn(Map.of(violacao.chaveEstavel(), violacao));
        when(avisoRepositorio.findAll())
                .thenReturn(List.of())
                .thenReturn(List.of(existente));
        when(avisoRepositorio.findAllByOrderByAtualizadoEmDesc()).thenReturn(List.of(existente));

        servico.revalidarInventario("teste");
        servico.revalidarInventario("teste");

        verify(avisoRepositorio, times(1)).saveAndFlush(any(AvisoEstivagemPatio.class));
    }

    @Test
    void deveResolverSomenteQuandoRevalidacaoNaoEncontrarMaisViolacao() {
        AvisoEstivagemPatio aviso = avisoAtivo();
        when(detector.detectar()).thenReturn(Map.of());
        when(avisoRepositorio.findAll()).thenReturn(List.of(aviso));
        when(avisoRepositorio.findAllByOrderByAtualizadoEmDesc()).thenReturn(List.of(aviso));

        servico.revalidarInventario("teste");

        assertEquals(StatusAvisoEstivagemPatio.RESOLVIDO, aviso.getStatus());
        verify(historicoRepositorio).save(any(HistoricoAvisoEstivagemPatio.class));
    }

    @Test
    void deveReabrirMesmaOcorrenciaQuandoViolacaoRecorrer() {
        AvisoEstivagemPatio aviso = avisoAtivo();
        aviso.setStatus(StatusAvisoEstivagemPatio.RESOLVIDO);
        aviso.setRecorrencias(0);
        ViolacaoDetectada violacao = violacao();
        when(detector.detectar()).thenReturn(Map.of(violacao.chaveEstavel(), violacao));
        when(avisoRepositorio.findAll()).thenReturn(List.of(aviso));
        when(avisoRepositorio.findAllByOrderByAtualizadoEmDesc()).thenReturn(List.of(aviso));

        servico.revalidarInventario("teste");

        assertEquals(StatusAvisoEstivagemPatio.REABERTO, aviso.getStatus());
        assertEquals(1, aviso.getRecorrencias());
        verify(avisoRepositorio, never()).saveAndFlush(any(AvisoEstivagemPatio.class));
    }

    @Test
    void deveBloquearDispatchParaDestinoComAvisoCriticoAtivo() {
        AvisoEstivagemPatio aviso = avisoAtivo();
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setLinhaDestino(2);
        ordem.setColunaDestino(3);
        ordem.setCamadaDestino("2");
        when(avisoRepositorio.findBySeveridadeAndStatusIn(
                any(SeveridadeAvisoEstivagemPatio.class), any()))
                .thenReturn(List.of(aviso));

        assertThrows(ResponseStatusException.class, () -> servico.validarOperacaoPlanejada(ordem));
    }

    private AvisoEstivagemPatio avisoAtivo() {
        AvisoEstivagemPatio aviso = new AvisoEstivagemPatio();
        aviso.setId(10L);
        aviso.setChaveEstavel("CONT001:20:PESO");
        aviso.setCodigoUnidade("CONT001");
        aviso.setPosicaoId(20L);
        aviso.setLinha(2);
        aviso.setColuna(3);
        aviso.setCamada("2");
        aviso.setRegra(TipoRegraEstivagemPatio.PESO);
        aviso.setSeveridade(SeveridadeAvisoEstivagemPatio.CRITICA);
        aviso.setStatus(StatusAvisoEstivagemPatio.ABERTO);
        aviso.setDescricao("Peso incompatível");
        aviso.setRecorrencias(0);
        return aviso;
    }

    private ViolacaoDetectada violacao() {
        return new ViolacaoDetectada(
                "CONT001:20:PESO",
                "CONT001",
                20L,
                "A",
                2,
                3,
                "2",
                TipoRegraEstivagemPatio.PESO,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Peso incompatível",
                "40 t",
                "30 t",
                "Realocar");
    }
}
