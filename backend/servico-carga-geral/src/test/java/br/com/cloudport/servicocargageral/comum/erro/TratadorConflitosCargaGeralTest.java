package br.com.cloudport.servicocargageral.comum.erro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class TratadorConflitosCargaGeralTest {

    private final TratadorConflitosCargaGeral tratador = new TratadorConflitosCargaGeral();

    @Test
    void deveRetornarConflitoSeguroSemDetalhesDoBanco() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/carga-geral/conhecimentos");
        request.addHeader("X-Correlation-Id", "corr-err40");
        ConflitoCadastroCargaException exception = new ConflitoCadastroCargaException(
                "BILL_OF_LADING_DUPLICADO",
                "Já existe um Bill of Lading com esse número.",
                new IllegalStateException("uk_conhecimento_carga_numero"));

        ResponseEntity<Map<String, Object>> resposta = tratador.tratarConflitoCadastro(exception, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-err40");
        assertThat(resposta.getBody()).containsEntry("codigo", "BILL_OF_LADING_DUPLICADO");
        assertThat(resposta.getBody()).containsEntry("mensagem", "Já existe um Bill of Lading com esse número.");
        assertThat(resposta.getBody().toString()).doesNotContain("uk_conhecimento_carga_numero");
    }
}
