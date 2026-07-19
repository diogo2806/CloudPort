package br.com.cloudport.servicogate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.cloudport.servicogate.security.TransportadoraSynchronizationFilter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigTest {

    @Test
    void deveRecusarSegredoJwtMenorQue256Bits() {
        SecurityConfig configuracao = new SecurityConfig(
                mock(TransportadoraSynchronizationFilter.class),
                "segredo-curto",
                "http://localhost:4200");

        assertThrows(IllegalStateException.class, configuracao::jwtDecoder);
    }

    @Test
    void deveLiberarCabecalhoDeCorrelacaoNoCors() {
        SecurityConfig configuracao = criarConfiguracao();

        CorsConfiguration cors = configuracao.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(cors);
        assertTrue(cors.getAllowedHeaders().contains("X-Correlation-Id"));
        assertTrue(cors.getExposedHeaders().contains("X-Correlation-Id"));
    }

    @Test
    void deveRemoverRoleTransportadoraQuandoPerfilPrincipalForRoot() {
        Jwt jwt = criarJwt(
                Arrays.asList("ROLE_ROOT", "ROLE_ADMIN_PORTO", "ROLE_TRANSPORTADORA"),
                "ROLE_ROOT"
        );

        Collection<String> authorities = converterAutoridades(jwt);

        assertThat(authorities)
                .contains("ROLE_ROOT", "ROLE_ADMIN_PORTO")
                .doesNotContain("ROLE_TRANSPORTADORA");
    }

    @Test
    void deveManterRoleTransportadoraQuandoPerfilPrincipalForTransportadora() {
        Jwt jwt = criarJwt(
                Arrays.asList("ROLE_TRANSPORTADORA"),
                "ROLE_TRANSPORTADORA"
        );

        Collection<String> authorities = converterAutoridades(jwt);

        assertThat(authorities).containsExactly("ROLE_TRANSPORTADORA");
    }

    @Test
    void deveManterCompatibilidadeComTokenSemPerfilPrincipal() {
        Jwt jwt = criarJwt(Arrays.asList("ROLE_TRANSPORTADORA"), null);

        Collection<String> authorities = converterAutoridades(jwt);

        assertThat(authorities).containsExactly("ROLE_TRANSPORTADORA");
    }

    private Collection<String> converterAutoridades(Jwt jwt) {
        JwtAuthenticationConverter converter = criarConfiguracao().jwtAuthenticationConverter();
        return converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    private Jwt criarJwt(Collection<String> roles, String perfil) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("admin@cloudports.com.br")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("roles", roles);
        if (perfil != null) {
            builder.claim("perfil", perfil);
        }
        return builder.build();
    }

    private SecurityConfig criarConfiguracao() {
        return new SecurityConfig(
                mock(TransportadoraSynchronizationFilter.class),
                "cloudport-test-jwt-secret-with-32-bytes",
                "http://localhost:4200");
    }
}
