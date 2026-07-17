package br.com.cloudport.servicoautenticacao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    @Test
    void deveCalcularExpiracaoAPartirDoInstanteAbsoluto() {
        Instant emitidoEm = Instant.parse("2026-07-17T12:00:00Z");
        Clock clock = Clock.fixed(emitidoEm, ZoneId.of("Pacific/Auckland"));
        TokenService tokenService = new TokenService(
                "segredo-de-teste-com-pelo-menos-256-bits-1234567890",
                Duration.ofMinutes(90),
                clock);

        assertEquals(emitidoEm.plus(Duration.ofMinutes(90)),
                tokenService.calcularExpiracao(emitidoEm));
    }
}
