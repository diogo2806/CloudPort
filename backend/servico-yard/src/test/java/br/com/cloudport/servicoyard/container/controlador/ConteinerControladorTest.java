package br.com.cloudport.servicoyard.container.controlador;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.cloudport.servicoyard.testes.BaseIntegracaoPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConteinerControladorTest extends BaseIntegracaoPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void deveRegistrarEConsultarDetalhe() throws Exception {
        Map<String, Object> registro = new HashMap<>();
        registro.put("identificacao", "CP-010");
        registro.put("posicaoPatio", "P3-D");
        registro.put("tipoCarga", TipoCargaConteiner.PERIGOSO.name());
        registro.put("pesoToneladas", new BigDecimal("15.2"));
        registro.put("restricoes", "Manter isolado");

        String resposta = mockMvc.perform(post("/yard/conteineres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificacao").value("CP-010"))
                .andReturn().getResponse().getContentAsString();

        ConteinerRespostaDetalhe detalhe = objectMapper.readValue(resposta, ConteinerRespostaDetalhe.class);

        mockMvc.perform(get("/yard/conteineres/" + detalhe.getIdentificador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicaoPatio").value("P3-D"));
    }

    @Test
    @Transactional
    void deveRegistrarTransferenciaEHitorico() throws Exception {
        Map<String, Object> registro = new HashMap<>();
        registro.put("identificacao", "CP-011");
        registro.put("posicaoPatio", "P4-A");
        registro.put("tipoCarga", TipoCargaConteiner.SECO.name());
        registro.put("pesoToneladas", new BigDecimal("20.0"));

        String resposta = mockMvc.perform(post("/yard/conteineres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ConteinerRespostaDetalhe detalhe = objectMapper.readValue(resposta, ConteinerRespostaDetalhe.class);

        Map<String, Object> transferencia = new HashMap<>();
        transferencia.put("posicaoDestino", "P5-B");
        transferencia.put("motivo", "Reorganização");
        transferencia.put("responsavel", "Supervisor");

        mockMvc.perform(post("/yard/conteineres/" + detalhe.getIdentificador() + "/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferencia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicaoPatio").value("P5-B"));

        mockMvc.perform(get("/yard/conteineres/" + detalhe.getIdentificador() + "/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].descricao", containsString("Transferência registrada")));
    }

    private static class ConteinerRespostaDetalhe {
        private Long identificador;

        public Long getIdentificador() {
            return identificador;
        }

        public void setIdentificador(Long identificador) {
            this.identificador = identificador;
        }
    }
}
