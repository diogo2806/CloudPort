package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
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
    private static final String CAMINHO_LINE_UP = "/public/line-up-navios";
    private static final String CAMINHO_WS_PATIO = "/ws/patio";
    private static final String CAMINHO_WS_RECURSOS = "/ws/recursos";
    private static final String CAMINHO_WS_EDI = "/ws/edi";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePermitirLineUpAnonimo() throws Exception {
        mockMvc.perform(get(CAMINHO_LINE_UP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publico").value(true));
    }

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
    void deveRejeitarHandshakePatioAnonimo() throws Exception {
        mockMvc.perform(get(CAMINHO_WS_PATIO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarHandshakePatioParaPerfilSemEscopo() throws Exception {
        mockMvc.perform(get(CAMINHO_WS_PATIO)
                        .with(jwtComRole("USUARIO_CAP")))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirHandshakePatioParaOperador() throws Exception {
        mockMvc.perform(get(CAMINHO_WS_PATIO)
                        .with(jwtComRole("OPERADOR_PATIO")))
                .andExpect(status().isOk());
    }

    @Test
    void deveRejeitarHandshakeRecursosParaOperadorGate() throws Exception {
        mockMvc.perform(get(CAMINHO_WS_RECURSOS)
                        .with(jwtComRole("OPERADOR_GATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirHandshakeRecursosParaPlanejador() throws Exception {
        mockMvc.perform(get(CAMINHO_WS_RECURSOS)
                        .with(jwtComRole("PLANEJADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void devePermitirHandshakeEdiParaServicoSiderurgico() throws Exception {
        mockMvc.perform(get(CAMINHO_WS_EDI)
                        .with(jwtComRole("SERVICE_SIDERURGICO")))
                .andExpect(status().isOk());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtComRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @RestController
    static class ControladorSec10 {

        @GetMapping(CAMINHO_LINE_UP)
        Map<String, Object> lineUp() {
            return Map.of("publico", true);
        }

        @GetMapping({CAMINHO_WS_PATIO, CAMINHO_WS_RECURSOS, CAMINHO_WS_EDI})
        Map<String, Object> websocket(Authentication authentication) {
            return Map.of("principal", authentication.getName());
        }

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
