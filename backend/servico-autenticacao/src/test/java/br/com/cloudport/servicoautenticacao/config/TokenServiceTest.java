package br.com.cloudport.servicoautenticacao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static final String SECRET = "segredo-de-teste-com-pelo-menos-256-bits-1234567890";
    private static final Instant EMITIDO_EM = Instant.parse("2026-07-17T12:00:00Z");

    @Test
    void deveCalcularExpiracaoAPartirDoInstanteAbsoluto() {
        TokenService tokenService = criarTokenService();

        assertEquals(EMITIDO_EM.plus(Duration.ofMinutes(90)),
                tokenService.calcularExpiracao(EMITIDO_EM));
    }

    @Test
    void naoDeveEmitirRoleTransportadoraSemVinculo() {
        Usuario usuario = criarUsuario(null, null);

        Claims claims = lerClaims(criarTokenService().generateToken(usuario));
        Collection<?> roles = claims.get("roles", Collection.class);

        assertTrue(roles.contains("ROLE_ROOT"));
        assertFalse(roles.contains("ROLE_TRANSPORTADORA"));
    }

    @Test
    void deveEmitirRoleTransportadoraQuandoExisteVinculo() {
        Usuario usuario = criarUsuario("12345678000199", "Transportadora Teste");

        Claims claims = lerClaims(criarTokenService().generateToken(usuario));
        Collection<?> roles = claims.get("roles", Collection.class);

        assertTrue(roles.contains("ROLE_TRANSPORTADORA"));
        assertEquals("12345678000199", claims.get("transportadoraDocumento", String.class));
        assertEquals("Transportadora Teste", claims.get("transportadoraNome", String.class));
    }

    private TokenService criarTokenService() {
        Clock clock = Clock.fixed(EMITIDO_EM, ZoneId.of("Pacific/Auckland"));
        return new TokenService(SECRET, Duration.ofMinutes(90), clock);
    }

    private Usuario criarUsuario(String documento, String nomeTransportadora) {
        Set<UsuarioPapel> papeis = Set.of(
                new UsuarioPapel(new Papel("ROLE_ROOT")),
                new UsuarioPapel(new Papel("ROLE_TRANSPORTADORA")));
        return new Usuario(
                "root@cloudports.com.br",
                "senha",
                "Root do sistema",
                documento,
                nomeTransportadora,
                papeis);
    }

    private Claims lerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
