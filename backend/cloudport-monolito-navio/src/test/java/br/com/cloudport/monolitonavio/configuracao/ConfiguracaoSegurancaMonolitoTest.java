package br.com.cloudport.monolitonavio.configuracao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.cloudport.monolitonavio.seguranca.InternalServiceAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class ConfiguracaoSegurancaMonolitoTest {

    @Test
    void devePermitirCabecalhosDosDoisModulos() {
        ConfiguracaoSegurancaMonolito configuracao = new ConfiguracaoSegurancaMonolito(
                "chave-de-teste-com-mais-de-trinta-e-dois-bytes",
                "http://localhost:4200,http://localhost:4201",
                mock(InternalServiceAuthenticationFilter.class));

        CorsConfiguration cors = configuracao.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/visitas-navio"));

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:4200"));
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:4201"));
        assertTrue(cors.getAllowedHeaders().contains("X-Correlation-Id"));
        assertTrue(cors.getAllowedHeaders().contains(InternalServiceAuthenticationFilter.HEADER_SERVICE_KEY));
        assertTrue(cors.getExposedHeaders().contains("X-Correlation-Id"));
    }
}
