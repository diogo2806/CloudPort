package br.com.cloudport.servicogate.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.cloudport.servicogate.security.TransportadoraSynchronizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
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
        SecurityConfig configuracao = new SecurityConfig(
                mock(TransportadoraSynchronizationFilter.class),
                "cloudport-test-jwt-secret-with-32-bytes",
                "http://localhost:4200");

        CorsConfiguration cors = configuracao.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(cors);
        assertTrue(cors.getAllowedHeaders().contains("X-Correlation-Id"));
        assertTrue(cors.getExposedHeaders().contains("X-Correlation-Id"));
    }
}
