package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.config.SecurityConfig;
import br.com.cloudport.servicogate.dto.GateEventDTO;
import br.com.cloudport.servicogate.dto.ManualReleaseAction;
import br.com.cloudport.servicogate.dto.ManualReleaseRequest;
import br.com.cloudport.servicogate.security.TransportadoraSynchronizationFilter;
import br.com.cloudport.servicogate.service.GateFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GateFlowController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "cloudport.security.jwt.secret=test-secret",
        "cloudport.security.cors.allowed-origins=http://localhost:4200"
})
class GateFlowControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GateFlowService gateFlowService;

    @MockBean
    private TransportadoraSynchronizationFilter transportadoraSynchronizationFilter;

    @Test
    void liberarManual_retornaUnauthorized_semToken() throws Exception {
        ManualReleaseRequest request = new ManualReleaseRequest();
        request.setAcao(ManualReleaseAction.LIBERAR);
        String payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/gate/agendamentos/1/liberacao-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void liberarManual_permitidoParaOperador() throws Exception {
        ManualReleaseRequest request = new ManualReleaseRequest();
        request.setAcao(ManualReleaseAction.LIBERAR);
        request.setObservacao("Liberado");

        when(gateFlowService.liberarManual(eq(1L), any())).thenReturn(new GateEventDTO());

        mockMvc.perform(post("/gate/agendamentos/1/liberacao-manual")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void liberarManual_negadoParaPlanejador() throws Exception {
        ManualReleaseRequest request = new ManualReleaseRequest();
        request.setAcao(ManualReleaseAction.LIBERAR);

        mockMvc.perform(post("/gate/agendamentos/1/liberacao-manual")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
