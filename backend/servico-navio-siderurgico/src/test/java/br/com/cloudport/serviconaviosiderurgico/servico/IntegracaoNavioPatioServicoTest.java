package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.comum.IntegracaoYardIndisponivelException;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusConsultaYard;
import br.com.cloudport.serviconaviosiderurgico.dto.FilaPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoConsultaYardDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IntegracaoNavioPatioServicoTest {

    private static final Long VISITA_ID = 10L;

    @Mock
    private ItemOperacaoNavioRepositorio itemRepositorio;

    @Mock
    private ReservaPosicaoPatioNavioRepositorio reservaRepositorio;

    @Mock
    private VisitaNavioServico visitaServico;

    @Mock
    private PlanoEstivaNavioServico planoServico;

    @Mock
    private ReservaPatioNavioServico reservaPatioServico;

    @Mock
    private ValidadorIntegracaoNavioPatioServico validador;

    @Mock
    private SincronizadorStatusNavioPatioServico sincronizador;

    @Mock
    private OrdemPatioYardCliente ordemPatioYardCliente;

    @Test
    void deveDiferenciarRespostaVaziaLegitimaDeFalhaNasFilas() {
        when(ordemPatioYardCliente.listarFilasDaVisita(VISITA_ID)).thenReturn(List.of());

        ResultadoConsultaYardDTO<FilaPatioDaVisitaDTO> resultado = criarServico(false)
                .listarFilasOperacionaisDaVisita(VISITA_ID);

        assertThat(resultado.status()).isEqualTo(StatusConsultaYard.CONFIRMADA);
        assertThat(resultado.confirmado()).isTrue();
        assertThat(resultado.fonte()).isEqualTo("YARD");
        assertThat(resultado.dados()).isEmpty();
        assertThat(resultado.motivoDegradacao()).isNull();
    }

    @Test
    void deveRetornar503QuandoConsultaObrigatoriaDeFilasFalhar() {
        when(ordemPatioYardCliente.listarFilasDaVisita(VISITA_ID))
                .thenThrow(new IllegalStateException("Yard fora do ar"));

        assertThatThrownBy(() -> criarServico(false).listarFilasOperacionaisDaVisita(VISITA_ID))
                .isInstanceOf(IntegracaoYardIndisponivelException.class)
                .satisfies(ex -> assertThat(((IntegracaoYardIndisponivelException) ex).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void deveIdentificarFilasDerivadasComoDegradadasSomenteEmContingencia() {
        when(ordemPatioYardCliente.listarFilasDaVisita(VISITA_ID))
                .thenThrow(new IllegalStateException("Yard fora do ar"));
        when(itemRepositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(VISITA_ID))
                .thenReturn(List.of());

        ResultadoConsultaYardDTO<FilaPatioDaVisitaDTO> resultado = criarServico(true)
                .listarFilasOperacionaisDaVisita(VISITA_ID);

        assertThat(resultado.status()).isEqualTo(StatusConsultaYard.DEGRADADA);
        assertThat(resultado.confirmado()).isFalse();
        assertThat(resultado.fonte()).isEqualTo("DERIVACAO_LOCAL_CONTINGENCIA");
        assertThat(resultado.dados()).isEmpty();
        assertThat(resultado.motivoDegradacao()).contains("Yard obrigatorio indisponivel");
    }

    @Test
    void deveRetornar503QuandoConsultaObrigatoriaDeCoberturaFalhar() {
        when(ordemPatioYardCliente.listarOrdensSemCobertura(VISITA_ID))
                .thenThrow(new IllegalStateException("Yard fora do ar"));

        assertThatThrownBy(() -> criarServico(false).listarOrdensSemCoberturaDaVisita(VISITA_ID))
                .isInstanceOf(IntegracaoYardIndisponivelException.class)
                .satisfies(ex -> assertThat(((IntegracaoYardIndisponivelException) ex).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void deveIdentificarCoberturaDerivadaComoDegradadaSomenteEmContingencia() {
        when(ordemPatioYardCliente.listarOrdensSemCobertura(VISITA_ID))
                .thenThrow(new IllegalStateException("Yard fora do ar"));
        when(itemRepositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(VISITA_ID))
                .thenReturn(List.of());

        ResultadoConsultaYardDTO<OrdemPatioDaVisitaDTO> resultado = criarServico(true)
                .listarOrdensSemCoberturaDaVisita(VISITA_ID);

        assertThat(resultado.status()).isEqualTo(StatusConsultaYard.DEGRADADA);
        assertThat(resultado.confirmado()).isFalse();
        assertThat(resultado.fonte()).isEqualTo("DERIVACAO_LOCAL_CONTINGENCIA");
        assertThat(resultado.dados()).isEmpty();
        assertThat(resultado.motivoDegradacao()).contains("Yard obrigatorio indisponivel");
    }

    private IntegracaoNavioPatioServico criarServico(boolean contingenciaHabilitada) {
        return new IntegracaoNavioPatioServico(
                itemRepositorio,
                reservaRepositorio,
                visitaServico,
                planoServico,
                reservaPatioServico,
                validador,
                sincronizador,
                ordemPatioYardCliente,
                contingenciaHabilitada
        );
    }
}
