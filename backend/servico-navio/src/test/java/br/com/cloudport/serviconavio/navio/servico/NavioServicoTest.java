package br.com.cloudport.serviconavio.navio.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.navio.dto.CadastroNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavioServicoTest {

    private NavioRepositorio navioRepositorio;
    private SanitizadorEntrada sanitizadorEntrada;
    private ApplicationEventPublisher eventPublisher;
    private NavioServico navioServico;

    @BeforeEach
    void preparar() {
        navioRepositorio = mock(NavioRepositorio.class);
        sanitizadorEntrada = mock(SanitizadorEntrada.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        when(sanitizadorEntrada.limparTextoObrigatorio(anyString(), anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(sanitizadorEntrada.limparTexto(anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        navioServico = new NavioServico(navioRepositorio, sanitizadorEntrada, eventPublisher);
    }

    private CadastroNavioDTO cadastroValido() {
        CadastroNavioDTO dto = new CadastroNavioDTO();
        dto.setNome("Navio Teste");
        dto.setCodigoImo("imo1234567");
        dto.setPaisBandeira("Brasil");
        dto.setEmpresaArmadora("Armadora X");
        dto.setCapacidadeTeu(5000);
        dto.setLoaMetros(new BigDecimal("294.00"));
        dto.setCaladoMaximoMetros(new BigDecimal("14.50"));
        dto.setCallSign("PWXY");
        return dto;
    }

    @Test
    void registrarNormalizaCodigoImoEPreservaAtributosFisicos() {
        when(navioRepositorio.existsByCodigoImoIgnoreCase(anyString())).thenReturn(false);
        when(navioRepositorio.save(any(Navio.class))).thenAnswer(invocacao -> {
            Navio navio = invocacao.getArgument(0);
            navio.setIdentificador(1L);
            return navio;
        });

        NavioDetalheDTO resultado = navioServico.registrar(cadastroValido());

        assertThat(resultado).isNotNull();
        ArgumentCaptor<Navio> capturado = ArgumentCaptor.forClass(Navio.class);
        verify(navioRepositorio).save(capturado.capture());
        Navio salvo = capturado.getValue();
        assertThat(salvo.getCodigoImo()).isEqualTo("IMO1234567");
        assertThat(salvo.getNome()).isEqualTo("Navio Teste");
        assertThat(salvo.getLoaMetros()).isEqualByComparingTo("294.00");
        assertThat(salvo.getCaladoMaximoMetros()).isEqualByComparingTo("14.50");
        assertThat(salvo.getCallSign()).isEqualTo("PWXY");
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void registrarRejeitaCodigoImoDuplicado() {
        when(navioRepositorio.existsByCodigoImoIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> navioServico.registrar(cadastroValido()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(navioRepositorio, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void buscarDetalheLancaNotFoundQuandoNaoExiste() {
        when(navioRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> navioServico.buscarDetalhe(99L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
