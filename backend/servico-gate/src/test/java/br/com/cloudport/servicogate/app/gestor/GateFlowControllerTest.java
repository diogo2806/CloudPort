package br.com.cloudport.servicogate.app.gestor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicogate.app.gestor.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.app.gestor.GateFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GateFlowController.class)
class GateFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GateFlowService gateFlowService;

    @Test
    @DisplayName("Deve bloquear chamadas sem autenticação no fluxo de entrada")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        GateFlowRequest request = new GateFlowRequest();
        request.setPlaca("ABC1234");
        request.setTimestamp(LocalDateTime.now());

        mockMvc.perform(post("/gate/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve aceitar eventos de entrada para usuário com perfil de operador")
    @WithMockUser(roles = "OPERADOR_GATE")
    void devePermitirEntradaParaOperador() throws Exception {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(10L);
        agendamento.setCodigo("AG-100");
        GateDecisionDTO decision = GateDecisionDTO.autorizado(StatusGate.LIBERADO, agendamento, null,
                "Entrada liberada");
        when(gateFlowService.registrarEntrada(any())).thenReturn(decision);

        GateFlowRequest request = new GateFlowRequest();
        request.setPlaca("ABC1234");
        request.setTimestamp(LocalDateTime.now());
        request.setOperador("joao");

        mockMvc.perform(post("/gate/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(true))
                .andExpect(jsonPath("$.agendamentoId").value(10L))
                .andExpect(jsonPath("$.mensagem").value("Entrada liberada"));
    }
}

