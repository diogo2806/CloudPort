package br.com.cloudport.servicocargageral.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicocargageral.configuracao.ConfiguracaoSeguranca;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoConhecimento;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ConhecimentoDetalhe;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarConhecimentoRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarReferenciaRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.DashboardResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ReferenciaResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarMovimentacaoRequest;
import br.com.cloudport.servicocargageral.servico.CargaGeralServico;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CargaGeralControlador.class)
@Import(ConfiguracaoSeguranca.class)
@TestPropertySource(properties = {
        "cloudport.security.jwt.secret=0123456789abcdef0123456789abcdef",
        "cloudport.security.cors.allowed-origins=http://localhost:4200"
})
class CargaGeralControladorSegurancaTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CargaGeralServico cargaGeralServico;

    @Test
    void consultaAnonimaRetornaUnauthorizedAntesDoServico() throws Exception {
        mockMvc.perform(get("/api/carga-geral/dashboard"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void transportadoraNaoPodeConsultarInventarioGlobal() throws Exception {
        mockMvc.perform(get("/api/carga-geral/dashboard")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSPORTADORA"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void planejadorPodeConsultarCargaGeral() throws Exception {
        when(cargaGeralServico.obterDashboard()).thenReturn(new DashboardResposta(
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        ));

        mockMvc.perform(get("/api/carga-geral/dashboard")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR"))))
                .andExpect(status().isOk());

        verify(cargaGeralServico).obterDashboard();
    }

    @Test
    void operadorGateNaoPodeCriarConhecimento() throws Exception {
        mockMvc.perform(post("/api/carga-geral/conhecimentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConhecimento()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void planejadorPodeCriarConhecimento() throws Exception {
        ConhecimentoDetalhe criado = new ConhecimentoDetalhe(
                ID,
                "BL-SEC80",
                TipoOperacaoConhecimento.EXPORTACAO,
                StatusConhecimentoCarga.RASCUNHO,
                "Embarcador",
                "Consignatario",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of()
        );
        when(cargaGeralServico.criarConhecimento(any(CriarConhecimentoRequest.class))).thenReturn(criado);

        mockMvc.perform(post("/api/carga-geral/conhecimentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConhecimento()))
                .andExpect(status().isCreated());

        verify(cargaGeralServico).criarConhecimento(any(CriarConhecimentoRequest.class));
    }

    @Test
    void planejadorNaoPodeRegistrarMovimentacao() throws Exception {
        mockMvc.perform(post("/api/carga-geral/lotes/{id}/movimentacoes", ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadMovimentacao()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void operadorGatePodeRegistrarMovimentacao() throws Exception {
        mockMvc.perform(post("/api/carga-geral/lotes/{id}/movimentacoes", ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadMovimentacao()))
                .andExpect(status().isOk());

        verify(cargaGeralServico).registrarMovimentacao(eq(ID), any(RegistrarMovimentacaoRequest.class));
    }

    @Test
    void planejadorNaoPodeRegistrarAvaria() throws Exception {
        mockMvc.perform(post("/api/carga-geral/lotes/{id}/avarias", ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadAvaria()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void operadorGatePodeRegistrarAvaria() throws Exception {
        mockMvc.perform(post("/api/carga-geral/lotes/{id}/avarias", ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadAvaria()))
                .andExpect(status().isOk());

        verify(cargaGeralServico).registrarAvaria(eq(ID), any(RegistrarAvariaRequest.class));
    }

    @Test
    void planejadorNaoPodeManterReferencia() throws Exception {
        mockMvc.perform(post("/api/carga-geral/referencias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadReferencia()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void administradorPodeManterReferencia() throws Exception {
        ReferenciaResposta criada = new ReferenciaResposta(
                ID,
                CategoriaReferenciaCarga.COMMODITY,
                "ACO",
                "Aco",
                null,
                true,
                OffsetDateTime.now()
        );
        when(cargaGeralServico.criarReferencia(any(CriarReferenciaRequest.class))).thenReturn(criada);

        mockMvc.perform(post("/api/carga-geral/referencias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PORTO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadReferencia()))
                .andExpect(status().isCreated());

        verify(cargaGeralServico).criarReferencia(any(CriarReferenciaRequest.class));
    }

    private String payloadConhecimento() {
        return """
                {
                  "numero": "BL-SEC80",
                  "tipoOperacao": "EXPORTACAO",
                  "embarcador": "Embarcador",
                  "consignatario": "Consignatario"
                }
                """;
    }

    private String payloadMovimentacao() {
        return """
                {
                  "tipo": "RECEBIMENTO",
                  "quantidade": 1,
                  "volumeM3": 1,
                  "pesoKg": 100,
                  "usuario": "operador"
                }
                """;
    }

    private String payloadAvaria() {
        return """
                {
                  "codigoAvaria": "AMASSADO",
                  "descricaoAvaria": "Avaria identificada na inspecao"
                }
                """;
    }

    private String payloadReferencia() {
        return """
                {
                  "categoria": "COMMODITY",
                  "codigo": "ACO",
                  "descricao": "Aco",
                  "ativo": true
                }
                """;
    }
}
