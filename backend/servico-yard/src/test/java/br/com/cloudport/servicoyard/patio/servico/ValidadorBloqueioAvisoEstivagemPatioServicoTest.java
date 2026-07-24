package br.com.cloudport.servicoyard.patio.servico;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.EstadoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.repositorio.AvisoEstivagemPatioRepositorio;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ValidadorBloqueioAvisoEstivagemPatioServicoTest {

    @Mock
    private AvisoEstivagemPatioServico avisoServico;

    @Mock
    private AvisoEstivagemPatioRepositorio avisoRepositorio;

    @InjectMocks
    private ValidadorBloqueioAvisoEstivagemPatioServico validador;

    private AvisoEstivagemPatio avisoCritico;

    @BeforeEach
    void preparar() {
        avisoCritico = new AvisoEstivagemPatio();
        avisoCritico.setCodigoUnidade("MSCU1234567");
        avisoCritico.setCodigoPosicao("3/7/2");
        avisoCritico.setEstado(EstadoAvisoEstivagemPatio.ABERTO);
        avisoCritico.setSeveridade(SeveridadeAvisoEstivagemPatio.CRITICA);
        avisoCritico.setBloqueiaOperacao(true);
        when(avisoServico.reavaliarUnidade("MSCU1234567", "sistema-yard"))
                .thenReturn(List.of(avisoCritico));
    }

    @Test
    void devePermitirMovimentacaoCorretivaParaOutraPosicaoSegura() {
        when(avisoRepositorio.existsByCodigoPosicaoInAndSeveridadeAndBloqueiaOperacaoTrueAndEstadoIn(
                anyCollection(),
                eq(SeveridadeAvisoEstivagemPatio.CRITICA),
                anyCollection()))
                .thenReturn(false);

        assertDoesNotThrow(() -> validador.validar("MSCU1234567", "4/9/1"));
    }

    @Test
    void deveBloquearQuandoUnidadePermaneceNaPosicaoViolada() {
        assertThrows(
                ResponseStatusException.class,
                () -> validador.validar("MSCU1234567", "3/7/2"));
    }

    @Test
    void deveBloquearMovimentacaoParaDestinoComAvisoCritico() {
        when(avisoRepositorio.existsByCodigoPosicaoInAndSeveridadeAndBloqueiaOperacaoTrueAndEstadoIn(
                anyCollection(),
                eq(SeveridadeAvisoEstivagemPatio.CRITICA),
                anyCollection()))
                .thenReturn(true);

        assertThrows(
                ResponseStatusException.class,
                () -> validador.validar("MSCU1234567", "4/9/1"));
    }
}
