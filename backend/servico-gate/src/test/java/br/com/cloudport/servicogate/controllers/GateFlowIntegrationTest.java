package br.com.cloudport.servicogate.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicogate.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.service.GateFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:gate-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
class GateFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GateFlowService gateFlowService;

    @Test
    @DisplayName("Fluxo de saída deve exigir autenticação com perfis válidos")
    void saidaDeveExigirAutenticacao() throws Exception {
        mockMvc.perform(post("/gate/saida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("placa", "DEF5678"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Fluxo de saída com usuário autenticado deve responder 200")
    @WithMockUser(roles = {"ADMIN_PORTO"})
    void saidaAutenticada() throws Exception {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(22L);
        agendamento.setCodigo("AG-200");
        when(gateFlowService.registrarSaida(any())).thenReturn(
                GateDecisionDTO.autorizado(StatusGate.FINALIZADO, agendamento, null, "Saída registrada"));

        Map<String, Object> payload = Map.of(
                "placa", "DEF5678",
                "timestamp", LocalDateTime.now().toString(),
                "operador", "maria"
        );

        mockMvc.perform(post("/gate/saida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }
}

