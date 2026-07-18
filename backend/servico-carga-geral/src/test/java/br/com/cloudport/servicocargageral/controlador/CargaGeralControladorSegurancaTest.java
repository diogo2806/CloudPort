package br.com.cloudport.servicocargageral.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicocargageral.configuracao.ConfiguracaoSeguranca;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarMovimentacaoRequest;
import br.com.cloudport.servicocargageral.servico.CargaGeralServico;
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

    private static final UUID LOTE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CargaGeralServico cargaGeralServico;

    @Test
    void deveRetornar401AntesDoServicoParaRequisicaoAnonima() throws Exception {
        mockMvc.perform(get("/api/carga-geral/dashboard"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void deveRetornar403ParaPerfilSemLeituraOperacional() throws Exception {
        mockMvc.perform(get("/api/carga-geral/dashboard")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSPORTADORA"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void devePermitirConsultaAoPlanejador() throws Exception {
        mockMvc.perform(get("/api/carga-geral/dashboard")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR"))))
                .andExpect(status().isOk());

        verify(cargaGeralServico).obterDashboard();
    }

    @Test
    void deveImpedirCriacaoDeConhecimentoPeloOperadorGate() throws Exception {
        mockMvc.perform(post("/api/carga-geral/conhecimentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numero": "BL-SEC80",
                                  "tipoOperacao": "EXPORTACAO",
                                  "embarcador": "Embarcador",
                                  "consignatario": "Consignatario"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }

    @Test
    void devePermitirMovimentacaoAoOperadorGate() throws Exception {
        mockMvc.perform(post("/api/carga-geral/lotes/{id}/movimentacoes", LOTE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "RECEBIMENTO",
                                  "quantidade": 1,
                                  "volumeM3": 1,
                                  "pesoKg": 100,
                                  "usuario": "operador"
                                }
                                """))
                .andExpect(status().isOk());

        verify(cargaGeralServico).registrarMovimentacao(eq(LOTE_ID), any(RegistrarMovimentacaoRequest.class));
    }

    @Test
    void deveImpedirManutencaoDeReferenciaPeloPlanejador() throws Exception {
        mockMvc.perform(post("/api/carga-geral/referencias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLANEJADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoria": "COMMODITY",
                                  "codigo": "ACO",
                                  "descricao": "Aco",
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cargaGeralServico);
    }
}
