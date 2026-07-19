package br.com.cloudport.servicogate.comum.erro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class TratadorExcecoesTest {

    @Test
    void deveRetornarForbiddenParaAcessoNegado() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gate/agendamentos");
        request.addHeader("X-Correlation-Id", "correlation-test");
        TratadorExcecoes tratadorExcecoes = new TratadorExcecoes();

        ResponseEntity<Map<String, Object>> response = tratadorExcecoes.acessoNegado(
                new AccessDeniedException("Transportadora autenticada sem vínculo válido"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("correlation-test");
        assertThat(response.getBody())
                .containsEntry("codigo", "ACESSO_NEGADO")
                .containsEntry("mensagem", "Transportadora autenticada sem vínculo válido")
                .containsEntry("correlationId", "correlation-test");
    }
}
