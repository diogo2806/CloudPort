package br.com.cloudport.servicocargageral.comum.erro;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
        DataIntegrityViolationException exception = violacao("conhecimento_carga_numero_key");

        ResponseEntity<Map<String, Object>> resposta = tratador.tratarFalhaIntegridade(exception, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-err40");
        assertThat(resposta.getBody()).containsEntry("codigo", "BILL_OF_LADING_DUPLICADO");
        assertThat(resposta.getBody()).containsEntry("mensagem", "Já existe um Bill of Lading com esse número.");
        assertThat(resposta.getBody().toString()).doesNotContain("conhecimento_carga_numero_key");
        assertThat(resposta.getBody().toString()).doesNotContain("violação de unicidade");
    }

    private DataIntegrityViolationException violacao(String restricao) {
        SQLException sqlException = new SQLException("violação de unicidade", "23505");
        ConstraintViolationException hibernateException =
                new ConstraintViolationException("restrição violada", sqlException, restricao);
        return new DataIntegrityViolationException("falha ao persistir", hibernateException);
    }
}
