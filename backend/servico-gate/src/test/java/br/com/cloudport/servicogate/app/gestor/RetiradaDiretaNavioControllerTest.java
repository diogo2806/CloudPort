package br.com.cloudport.servicogate.app.gestor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
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
        "spring.datasource.url=jdbc:h2:mem:retirada-direta-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
class RetiradaDiretaNavioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RetiradaDiretaNavioService service;

    @Test
    void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(post("/gate/retiradas-diretas-navio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OPERADOR_GATE")
    void deveRegistrarSaidaComOperadorAutorizado() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        RetiradaDiretaNavioDTO response = new RetiradaDiretaNavioDTO(
                1L,
                "AUT-2026-001",
                "TRATOR-CHASSI-001",
                "TRATOR",
                "VV-2026-009",
                "Cliente Teste",
                "12345678900",
                "FINALIZADA",
                "Finalizada",
                agora,
                "operador.gate",
                null,
                agora
        );
        when(service.processar(any())).thenReturn(response);

        mockMvc.perform(post("/gate/retiradas-diretas-navio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificadorCarga").value("TRATOR-CHASSI-001"))
                .andExpect(jsonPath("$.status").value("FINALIZADA"));
    }

    private Map<String, Object> payloadValido() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("codigoAutorizacao", "AUT-2026-001");
        payload.put("identificadorCarga", "TRATOR-CHASSI-001");
        payload.put("tipoCarga", "TRATOR");
        payload.put("visitaNavio", "VV-2026-009");
        payload.put("clienteNome", "Cliente Teste");
        payload.put("clienteDocumento", "12345678900");
        payload.put("documentosValidados", true);
        payload.put("liberacaoAduaneiraConfirmada", true);
        payload.put("cargaDescarregada", true);
        payload.put("condutorHabilitado", true);
        return payload;
    }
}
