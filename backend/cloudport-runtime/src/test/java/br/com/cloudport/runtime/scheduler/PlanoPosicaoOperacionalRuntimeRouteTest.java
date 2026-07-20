package br.com.cloudport.runtime.scheduler;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicoyard.scheduler.controller.PlanoPosicaoOperacionalControlador;
import br.com.cloudport.servicoyard.scheduler.servico.PlanoPosicaoOperacionalServico;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PlanoPosicaoOperacionalControlador.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanoPosicaoOperacionalRuntimeRouteTest {

    private static final String ROTA_PLANOS_POSICAO = "/api/scheduler/planos-posicao";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanoPosicaoOperacionalServico servico;

    @Test
    void deveExporListagemDePlanosDePosicaoNoRuntimeCanonico() throws Exception {
        when(servico.listar(null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(ROTA_PLANOS_POSICAO)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
