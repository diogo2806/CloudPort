package br.com.cloudport.servicoyard.scheduler.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.EventoVmtWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.scheduler.dto.PrevisaoDemandaYardDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PrevisaoDemandaYardServicoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 7, 23, 12, 0);
    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-07-23T12:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void deveProjetarMovimentosConcluidosProporcionalmenteAoHorizonte() {
        EventoVmtWorkInstructionRepositorio repositorio = repositorioComContagens(600, 168, 140);
        PrevisaoDemandaYardServico servico = new PrevisaoDemandaYardServico(repositorio, true, RELOGIO);

        PrevisaoDemandaYardDto seisHoras = servico.prever(6);
        PrevisaoDemandaYardDto vinteQuatroHoras = servico.prever(24);

        assertEquals(6, seisHoras.baselineDeterministico());
        assertEquals(8, seisHoras.demandaPrevista());
        assertEquals(24, vinteQuatroHoras.baselineDeterministico());
        assertEquals(29, vinteQuatroHoras.demandaPrevista());
        assertTrue(vinteQuatroHoras.demandaPrevista() > seisHoras.demandaPrevista());
        assertEquals(0.0, seisHoras.confianca());
        assertFalse(seisHoras.fallbackDeterministico());
        assertEquals("BASELINE_TEMPORAL_VMT", seisHoras.modelo());
    }

    @Test
    void deveUsarSomenteBaselineTemporalQuandoAjusteEstaDesativado() {
        EventoVmtWorkInstructionRepositorio repositorio = repositorioComContagens(600, 168, 140);
        PrevisaoDemandaYardServico servico = new PrevisaoDemandaYardServico(repositorio, false, RELOGIO);

        PrevisaoDemandaYardDto resposta = servico.prever(12);

        assertEquals(12, resposta.demandaPrevista());
        assertEquals(12, resposta.baselineDeterministico());
        assertEquals(720, resposta.duracaoPrevistaMinutos());
        assertEquals(0.0, resposta.confianca());
        assertTrue(resposta.fallbackDeterministico());
        assertTrue(resposta.explicacao().contains("Planos de posição cancelados, expirados ou acumulados não participam"));
    }

    @Test
    void deveInformarAmostraInsuficienteSemInventarConfianca() {
        EventoVmtWorkInstructionRepositorio repositorio = repositorioComContagens(4, 4, 0);
        PrevisaoDemandaYardServico servico = new PrevisaoDemandaYardServico(repositorio, true, RELOGIO);

        PrevisaoDemandaYardDto resposta = servico.prever(6);

        assertEquals(1, resposta.demandaPrevista());
        assertTrue(resposta.fallbackDeterministico());
        assertEquals(0.0, resposta.confianca());
        assertTrue(resposta.explicacao().contains("4 eventos físicos concluídos"));
    }

    private EventoVmtWorkInstructionRepositorio repositorioComContagens(
            long amostra28Dias,
            long ultimos7Dias,
            long seteDiasAnteriores) {
        EventoVmtWorkInstructionRepositorio repositorio = mock(EventoVmtWorkInstructionRepositorio.class);
        when(repositorio.countByTipoEventoInAndOcorridoEmGreaterThanEqualAndOcorridoEmLessThan(
                anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenAnswer(invocacao -> {
                    LocalDateTime inicio = invocacao.getArgument(1);
                    LocalDateTime fim = invocacao.getArgument(2);
                    if (inicio.equals(AGORA.minusDays(28)) && fim.equals(AGORA)) return amostra28Dias;
                    if (inicio.equals(AGORA.minusDays(7)) && fim.equals(AGORA)) return ultimos7Dias;
                    if (inicio.equals(AGORA.minusDays(14)) && fim.equals(AGORA.minusDays(7))) return seteDiasAnteriores;
                    return 0L;
                });
        return repositorio;
    }
}
