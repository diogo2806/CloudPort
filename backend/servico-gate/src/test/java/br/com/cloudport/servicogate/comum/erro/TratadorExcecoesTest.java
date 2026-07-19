package br.com.cloudport.servicogate.comum.erro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class TratadorExcecoesTest {

    @Test
    void deveRetornarForbiddenParaAcessoNegado() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cap/resumo");
        request.addHeader("X-Correlation-Id", "correlation-cap-403");
        TratadorExcecoes tratador = new TratadorExcecoes();

        ResponseEntity<Map<String, Object>> response = tratador.acessoNegado(
                new AccessDeniedException("Transportadora autenticada sem vínculo válido."),
                request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACESSO_NEGADO", response.getBody().get("codigo"));
        assertEquals("Transportadora autenticada sem vínculo válido.", response.getBody().get("mensagem"));
        assertEquals("correlation-cap-403", response.getBody().get("correlationId"));
        assertEquals("GET /cap/resumo", detalhes(response).get("rota"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> detalhes(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, Object>) response.getBody().get("detalhes");
    }
}
