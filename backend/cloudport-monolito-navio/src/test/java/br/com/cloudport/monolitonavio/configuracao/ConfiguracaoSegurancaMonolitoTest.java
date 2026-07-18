package br.com.cloudport.monolitonavio.configuracao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.cloudport.monolitonavio.seguranca.InternalServiceAuthenticationFilter;
import br.com.cloudport.serviconaviosiderurgico.configuracao.PublicApiClientAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class ConfiguracaoSegurancaMonolitoTest {

    @Test
    void devePermitirCabecalhosDosDoisModulos() {
        ConfiguracaoSegurancaMonolito configuracao = configuracao();

        CorsConfiguration cors = configuracao.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/visitas-navio"));

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:4200"));
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:4201"));
        assertTrue(cors.getAllowedHeaders().contains("X-Correlation-Id"));
        assertTrue(cors.getAllowedHeaders().contains(InternalServiceAuthenticationFilter.HEADER_SERVICE_KEY));
        assertTrue(cors.getExposedHeaders().contains("X-Correlation-Id"));
    }

    @Test
    void deveExigirCsrfEmComandoSemCredencialExplicita() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/usuarios");

        assertTrue(configuracao().csrfProtectionMatcher().matches(request));
    }

    @Test
    void naoDeveExigirCsrfEmComandoComJwtBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/usuarios");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-jwt");

        assertFalse(configuracao().csrfProtectionMatcher().matches(request));
    }

    @Test
    void naoDeveExigirCsrfNoLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");

        assertFalse(configuracao().csrfProtectionMatcher().matches(request));
    }

    @Test
    void naoDeveExigirCsrfComChaveDeServicoInterno() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/interna/processar");
        request.addHeader(InternalServiceAuthenticationFilter.HEADER_SERVICE_KEY, "chave-interna");

        assertFalse(configuracao().csrfProtectionMatcher().matches(request));
    }

    @Test
    void naoDeveExigirCsrfComCredenciaisDeClientePublico() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/public/v1/eventos");
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID, "cliente");
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET, "segredo");

        assertFalse(configuracao().csrfProtectionMatcher().matches(request));
    }

    private ConfiguracaoSegurancaMonolito configuracao() {
        return new ConfiguracaoSegurancaMonolito(
                "chave-de-teste-com-mais-de-trinta-e-dois-bytes",
                "http://localhost:4200,http://localhost:4201",
                mock(InternalServiceAuthenticationFilter.class),
                mock(PublicApiClientAuthenticationFilter.class));
    }
}
