package br.com.cloudport.servicoyard.seguranca;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicoyard.configuracao.ConfiguracaoSeguranca;
import br.com.cloudport.servicoyard.configuracao.InternalServiceAuthenticationFilter;
import br.com.cloudport.servicoyard.estivagembulk.controlador.EstivaBulkControlador;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ValidacaoPlanoBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.servico.NavioGranelConsultaServico;
import br.com.cloudport.servicoyard.estivagembulk.servico.PlanoEstivaBulkIdentidadeServico;
import br.com.cloudport.servicoyard.vesselplanner.controlador.VesselPlannerControlador;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import br.com.cloudport.servicoyard.vesselplanner.servico.ExecucaoSequenciaGuindasteServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.ReconciliacaoBaplieExecucaoServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.VesselPlannerServico;
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
        VesselPlannerControlador.class,
        EstivaBulkControlador.class
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
class PlanejamentoEstivaAutorizacaoControladorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VesselPlannerServico vesselPlannerServico;

    @MockBean
    private ExecucaoSequenciaGuindasteServico execucaoSequenciaGuindasteServico;

    @MockBean
    private ReconciliacaoBaplieExecucaoServico reconciliacaoBaplieExecucaoServico;

    @MockBean
    private PlanoEstivaBulkIdentidadeServico planoEstivaBulkServico;

    @MockBean
    private NavioGranelConsultaServico navioGranelConsultaServico;

    @Test
    void deveRetornar403QuandoOperadorTentarCriarPlanoDoVesselPlanner() throws Exception {
        mockMvc.perform(post("/api/vessel-planner/planos")
                        .with(jwtComRole("OPERADOR_PATIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bayPlanId\":1,\"visitaNavioId\":2}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vesselPlannerServico);
    }

    @Test
    void devePermitirCriacaoDoVesselPlannerParaPlanejador() throws Exception {
        when(vesselPlannerServico.criarPlanoDeBayPlan(1L, 2L)).thenReturn(new EstivagemPlanDto());

        mockMvc.perform(post("/api/vessel-planner/planos")
                        .with(jwtComRole("PLANEJADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bayPlanId\":1,\"visitaNavioId\":2}"))
                .andExpect(status().isCreated());

        verify(vesselPlannerServico).criarPlanoDeBayPlan(1L, 2L);
    }

    @Test
    void deveRetornar403QuandoPerfilForaDaMatrizTentarLerPlano() throws Exception {
        mockMvc.perform(get("/api/vessel-planner/planos/1")
                        .with(jwtComRole("TRANSPORTADORA")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vesselPlannerServico);
    }

    @Test
    void deveListarPlanosBulkPorNavioEVisitaParaPlanejador() throws Exception {
        when(planoEstivaBulkServico.listarPlanos(7L, 91L)).thenReturn(List.of());

        mockMvc.perform(get("/api/estivagem-bulk/planos")
                        .param("navioId", "7")
                        .param("visitaNavioId", "91")
                        .with(jwtComRole("PLANEJADOR")))
                .andExpect(status().isOk());

        verify(planoEstivaBulkServico).listarPlanos(7L, 91L);
    }

    @Test
    void deveListarModelosDeNavioParaPlanejador() throws Exception {
        NavioGranelDto modelo = new NavioGranelDto();
        modelo.setId(7L);
        modelo.setNome("Modelo Panamax");
        modelo.setTemplate(true);
        when(navioGranelConsultaServico.listarModelos()).thenReturn(List.of(modelo));

        mockMvc.perform(get("/api/estivagem-bulk/navios/templates")
                        .with(jwtComRole("PLANEJADOR")))
                .andExpect(status().isOk());

        verify(navioGranelConsultaServico).listarModelos();
    }

    @Test
    void deveRetornar403QuandoTransportadoraTentarListarModelosDeNavio() throws Exception {
        mockMvc.perform(get("/api/estivagem-bulk/navios/templates")
                        .with(jwtComRole("TRANSPORTADORA")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(navioGranelConsultaServico);
    }

    @Test
    void deveRetornar403QuandoOperadorTentarValidarPlanoBulk() throws Exception {
        mockMvc.perform(post("/api/estivagem-bulk/planos/1/validacao-completa")
                        .with(jwtComRole("OPERADOR_PATIO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(planoEstivaBulkServico);
    }

    @Test
    void devePermitirValidacaoDoPlanoBulkParaAdministrador() throws Exception {
        ValidacaoPlanoBulkDto validacao = new ValidacaoPlanoBulkDto();
        validacao.setAprovado(true);
        when(planoEstivaBulkServico.validarPlanoCompleto(1L)).thenReturn(validacao);

        mockMvc.perform(post("/api/estivagem-bulk/planos/1/validacao-completa")
                        .with(jwtComRole("ADMIN_PORTO")))
                .andExpect(status().isOk());

        verify(planoEstivaBulkServico).validarPlanoCompleto(1L);
    }

    private static RequestPostProcessor jwtComRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
