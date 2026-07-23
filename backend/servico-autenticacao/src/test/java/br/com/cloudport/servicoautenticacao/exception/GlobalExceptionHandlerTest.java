package br.com.cloudport.servicoautenticacao.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    @Test
    void deveRetornarForbiddenParaAcessoNegadoNoCap() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cap/resumo");
        request.addHeader("X-Correlation-Id", "correlation-cap-403");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, Object>> response = handler.acessoNegado(
                new AccessDeniedException("Acesso negado"),
                request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACESSO_NEGADO", response.getBody().get("codigo"));
        assertEquals("Acesso negado", response.getBody().get("mensagem"));
        assertEquals("correlation-cap-403", response.getBody().get("correlationId"));
        assertEquals("GET /cap/resumo", detalhes(response).get("rota"));
    }

    @Test
    void deveUsarMensagemPadraoQuandoAcessoNegadoNaoTemMensagem() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cap/resumo");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, Object>> response = handler.acessoNegado(
                new AccessDeniedException(null),
                request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Acesso negado.", response.getBody().get("mensagem"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> detalhes(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, Object>) response.getBody().get("detalhes");
    }
}
