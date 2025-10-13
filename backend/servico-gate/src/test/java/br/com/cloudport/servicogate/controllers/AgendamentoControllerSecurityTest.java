package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.config.SecurityConfig;
import br.com.cloudport.servicogate.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.dto.AgendamentoRequest;
import br.com.cloudport.servicogate.security.TransportadoraSynchronizationFilter;
import br.com.cloudport.servicogate.service.AgendamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AgendamentoController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "cloudport.security.jwt.secret=test-secret",
        "cloudport.security.cors.allowed-origins=http://localhost:4200"
})
class AgendamentoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgendamentoService agendamentoService;

    @MockBean
    private TransportadoraSynchronizationFilter transportadoraSynchronizationFilter;

    @Test
    void criarAgendamento_requerAutenticacao() throws Exception {
        mockMvc.perform(post("/gate/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarAgendamento_autorizadoParaAdmin() throws Exception {
        when(agendamentoService.criar(any())).thenReturn(new AgendamentoDTO());

        mockMvc.perform(post("/gate/agendamentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PORTO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void criarAgendamento_negadoParaTransportadora() throws Exception {
        mockMvc.perform(post("/gate/agendamentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSPORTADORA")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarAgendamentos_permitidoParaTransportadora() throws Exception {
        when(agendamentoService.buscar(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/gate/agendamentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSPORTADORA"))))
                .andExpect(status().isOk());
    }

    private AgendamentoRequest buildRequest() {
        AgendamentoRequest request = new AgendamentoRequest();
        request.setCodigo("AG-001");
        request.setTipoOperacao("IMPORTACAO");
        request.setStatus("CONFIRMADO");
        request.setTransportadoraId(1L);
        request.setMotoristaId(1L);
        request.setVeiculoId(1L);
        request.setJanelaAtendimentoId(1L);
        request.setHorarioPrevistoChegada(LocalDateTime.now().plusHours(2));
        request.setHorarioPrevistoSaida(LocalDateTime.now().plusHours(4));
        request.setObservacoes("Teste");
        return request;
    }
}
