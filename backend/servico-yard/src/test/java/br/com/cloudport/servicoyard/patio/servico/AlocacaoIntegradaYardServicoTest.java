package br.com.cloudport.servicoyard.patio.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoOtimizadaDto;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlocacaoIntegradaYardServicoTest {

    private OptimizadorYardService otimizador;
    private ValidadorYardPlacementService validador;
    private MapaPatioServico mapaPatio;
    private AlocacaoIntegradaYardServico servico;

    @BeforeEach
    void configurar() {
        otimizador = mock(OptimizadorYardService.class);
        validador = mock(ValidadorYardPlacementService.class);
        mapaPatio = mock(MapaPatioServico.class);
        servico = new AlocacaoIntegradaYardServico(otimizador, validador, mapaPatio);
    }

    @Test
    void devePersistirLoteValido() {
        ContainerOtimizacaoDto entrada = container(1L, "CONT001");
        when(otimizador.otimizarAlocacao(List.of(entrada))).thenReturn(List.of(posicao(1L, "CONT001")));
        doNothing().when(validador).validarAlocacao(any());

        var resposta = servico.alocar(List.of(entrada));

        assertEquals(1, resposta.totalAlocado());
        assertEquals(0, resposta.totalRejeitado());
        verify(mapaPatio).registrarOuAtualizarConteiner(any());
    }

    @Test
    void deveRetornarRejeicaoParcialComMotivo() {
        ContainerOtimizacaoDto valido = container(1L, "CONT001");
        ContainerOtimizacaoDto invalido = container(2L, "CONT002");
        when(otimizador.otimizarAlocacao(List.of(valido, invalido)))
                .thenReturn(List.of(posicao(1L, "CONT001"), posicao(2L, "CONT002")));
        doNothing().doThrow(new IllegalArgumentException("Berço incompatível"))
                .when(validador).validarAlocacao(any());

        var resposta = servico.alocar(List.of(valido, invalido));

        assertEquals(1, resposta.totalAlocado());
        assertEquals(1, resposta.totalRejeitado());
        assertEquals("Berço incompatível", resposta.resultados().get(1).motivo());
    }

    @Test
    void devePropagarFalhaDePersistenciaParaRollbackTransacional() {
        ContainerOtimizacaoDto entrada = container(1L, "CONT001");
        when(otimizador.otimizarAlocacao(List.of(entrada))).thenReturn(List.of(posicao(1L, "CONT001")));
        doThrow(new IllegalStateException("Banco indisponível"))
                .when(mapaPatio).registrarOuAtualizarConteiner(any());

        assertThrows(IllegalStateException.class, () -> servico.alocar(List.of(entrada)));
    }

    private ContainerOtimizacaoDto container(Long id, String codigo) {
        ContainerOtimizacaoDto dto = new ContainerOtimizacaoDto(id, codigo, LocalDateTime.now());
        dto.setTipoCarga("SECO");
        dto.setDestino("BERCO-1");
        return dto;
    }

    private PosicaoOtimizadaDto posicao(Long id, String codigo) {
        return new PosicaoOtimizadaDto(id, codigo, 1, 1, 1, 0, true, null);
    }
}
