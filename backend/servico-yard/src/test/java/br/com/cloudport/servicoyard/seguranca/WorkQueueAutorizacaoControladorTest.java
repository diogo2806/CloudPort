package br.com.cloudport.servicoyard.seguranca;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicoyard.configuracao.ConfiguracaoSeguranca;
import br.com.cloudport.servicoyard.configuracao.InternalServiceAuthenticationFilter;
import br.com.cloudport.servicoyard.patio.listatrabalho.controlador.WorkQueueOperacaoControlador;
import br.com.cloudport.servicoyard.patio.listatrabalho.controlador.WorkQueuePatioControlador;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AuditoriaComandoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.EventoOperacaoPatioPublicador;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueuePatioServico;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = {
        WorkQueuePatioControlador.class,
        WorkQueueOperacaoControlador.class
})
@Import({
        ConfiguracaoSeguranca.class,
        InternalServiceAuthenticationFilter.class
})
@TestPropertySource(properties = {
        "cloudport.security.jwt.secret=01234567890123456789012345678901",
        "cloudport.security.cors.allowed-origins=http://localhost:4200",
        "cloudport.security.internal-service-key="
})
class WorkQueueAutorizacaoControladorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkQueuePatioServico workQueuePatioServico;

    @MockBean
    private WorkQueueOperacaoServico workQueueOperacaoServico;

    @MockBean
    private AuditoriaComandoPatioServico auditoriaComandoPatioServico;

    @MockBean
    private EventoOperacaoPatioPublicador eventoOperacaoPatioPublicador;

    @Test
    void devePermitirConsultaParaOperadorGate() throws Exception {
        when(workQueuePatioServico.listar(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/yard/patio/work-queues")
                        .with(jwtComRole("OPERADOR_GATE")))
                .andExpect(status().isOk());

        verify(workQueuePatioServico).listar(null);
    }

    @Test
    void deveBloquearAdministracaoDaFilaParaOperadorGate() throws Exception {
        mockMvc.perform(patch("/yard/patio/work-queues/1/ativar")
                        .with(jwtComRole("OPERADOR_GATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"teste de autorizacao\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workQueuePatioServico, auditoriaComandoPatioServico,
                eventoOperacaoPatioPublicador);
    }

    @Test
    void deveBloquearAdministracaoDaFilaParaOperadorPatio() throws Exception {
        mockMvc.perform(patch("/yard/patio/work-queues/1/ativar")
                        .with(jwtComRole("OPERADOR_PATIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"teste de autorizacao\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workQueuePatioServico, auditoriaComandoPatioServico,
                eventoOperacaoPatioPublicador);
    }

    @Test
    void devePermitirAdministracaoDaFilaParaPlanejador() throws Exception {
        mockMvc.perform(patch("/yard/patio/work-queues/1/ativar")
                        .with(jwtComRole("PLANEJADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"ativacao planejada\"}"))
                .andExpect(status().isOk());

        verify(workQueuePatioServico).ativar(1L);
    }

    @Test
    void devePermitirDispatchParaOperadorPatio() throws Exception {
        mockMvc.perform(post("/yard/patio/work-queues/1/dispatch")
                        .with(jwtComRole("OPERADOR_PATIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"inicio do turno\"}"))
                .andExpect(status().isOk());

        verify(workQueueOperacaoServico).despachar(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(DispatchWorkQueueDto.class));
    }

    @Test
    void deveBloquearDispatchParaServiceNavio() throws Exception {
        mockMvc.perform(post("/yard/patio/work-queues/1/dispatch")
                        .with(jwtComRole("SERVICE_NAVIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"tentativa indevida\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workQueueOperacaoServico, eventoOperacaoPatioPublicador);
    }

    private static RequestPostProcessor jwtComRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
