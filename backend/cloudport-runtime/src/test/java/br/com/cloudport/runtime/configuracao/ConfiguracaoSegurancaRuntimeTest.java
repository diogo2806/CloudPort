package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.serviconavio.configuracao.InternalServiceAuthenticationFilter;
import br.com.cloudport.serviconaviosiderurgico.configuracao.PublicApiClientAuthenticationFilter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ConfiguracaoSegurancaRuntimeTest.ControladorSec10.class)
@Import({
        ConfiguracaoSegurancaRuntime.class,
        PublicApiClientAuthenticationFilter.class,
        ConfiguracaoSegurancaRuntimeTest.ConfiguracaoTeste.class
})
@TestPropertySource(properties = {
        "cloudport.security.jwt.secret=01234567890123456789012345678901",
        "cloudport.security.cors.allowed-origins=http://localhost:4200",
        "cloudport.security.public-api.clients=secretaria-portos:segredo-publico-com-32-caracteres"
})
class ConfiguracaoSegurancaRuntimeTest {

    private static final String CAMINHO_PUBLICO = "/api/public/v1/secretaria-clientes:sync";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRejeitarRotaPublicaSemCredenciaisDeCliente() throws Exception {
        mockMvc.perform(post(CAMINHO_PUBLICO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_PUBLICO_INVALIDO"));
    }

    @Test
    void deveAutenticarRotaPublicaComRoleExterna() throws Exception {
        mockMvc.perform(post(CAMINHO_PUBLICO)
                        .header(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID, "secretaria-portos")
                        .header(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET,
                                "segredo-publico-com-32-caracteres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal").value("client:secretaria-portos"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_INTEGRACAO_EXTERNA"));
    }

    @Test
    void deveAceitarCabecalhosPublicosNoPreflightCors() throws Exception {
        MvcResult resultado = mockMvc.perform(options(CAMINHO_PUBLICO)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "X-CloudPort-Client-Id, X-CloudPort-Client-Secret"))
                .andExpect(status().isOk())
                .andReturn();

        String cabecalhosPermitidos = resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
        assertThat(cabecalhosPermitidos).isNotNull();
        assertThat(cabecalhosPermitidos.toLowerCase())
                .contains("x-cloudport-client-id")
                .contains("x-cloudport-client-secret");
    }

    @Test
    void deveRejeitarSegredoJwtSentinelaNoRuntime() {
        ConfiguracaoSegurancaRuntime configuracao = new ConfiguracaoSegurancaRuntime(
                "chave-local-para-desenvolvimento-123456",
                "http://localhost:4200",
                mock(InternalServiceAuthenticationFilter.class),
                mock(PublicApiClientAuthenticationFilter.class)
        );

        assertThatThrownBy(configuracao::jwtDecoder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sentinela");
    }

    @RestController
    static class ControladorSec10 {

        @PostMapping(CAMINHO_PUBLICO)
        Map<String, Object> sincronizar(Authentication authentication) {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            return Map.of(
                    "principal", authentication.getName(),
                    "roles", roles
            );
        }
    }

    @TestConfiguration
    static class ConfiguracaoTeste {

        @Bean
        InternalServiceAuthenticationFilter internalServiceAuthenticationFilter() {
            return new InternalServiceAuthenticationFilter("");
        }
    }
}
