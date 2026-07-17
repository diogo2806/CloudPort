package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.config.SecurityConfig;
import br.com.cloudport.servicogate.security.TransportadoraSynchronizationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ControleAcessoPessoasController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "cloudport.security.jwt.secret=test-secret",
        "cloudport.security.cors.allowed-origins=http://localhost:4200"
})
class ControleAcessoPessoasControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ControleAcessoPessoasService controleAcessoPessoasService;

    @MockBean
    private TransportadoraSynchronizationFilter transportadoraSynchronizationFilter;

    @Test
    void registrarEntrada_retornaUnauthorizedSemToken() throws Exception {
        mockMvc.perform(post("/gate/pessoas/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadEntrada()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrarEntrada_permitidoParaOperadorGate() throws Exception {
        mockMvc.perform(post("/gate/pessoas/entradas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadEntrada()))
                .andExpect(status().isCreated());
    }

    @Test
    void registrarEntrada_negadoParaPlanejador() throws Exception {
        mockMvc.perform(post("/gate/pessoas/entradas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadEntrada()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarPresentes_permitidoParaPlanejador() throws Exception {
        when(controleAcessoPessoasService.listarPresentes()).thenReturn(List.of());

        mockMvc.perform(get("/gate/pessoas/presentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR"))))
                .andExpect(status().isOk());
    }

    private String payloadEntrada() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nome", "Maria da Silva",
                "documento", "12345678900",
                "tipoPessoa", "VISITANTE",
                "pontoAcesso", "Portaria principal"));
    }
}
