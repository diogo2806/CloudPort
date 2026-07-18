package br.com.cloudport.servicocargageral.comum.erro;

import br.com.cloudport.servicocargageral.controlador.CargaGeralControlador;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CargaGeralControlador.class)
public class TratadorConflitosCargaGeral {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarFalhaIntegridade(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        RuntimeException traduzida = TradutorConflitoCadastroCarga.traduzir(exception);
        if (!(traduzida instanceof ConflitoCadastroCargaException conflito)) {
            throw exception;
        }
        return tratarConflitoCadastro(conflito, request);
    }

    @ExceptionHandler(ConflitoCadastroCargaException.class)
    public ResponseEntity<Map<String, Object>> tratarConflitoCadastro(
            ConflitoCadastroCargaException exception,
            HttpServletRequest request) {
        String correlationId = request.getHeader(HEADER_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", request.getMethod() + " " + request.getRequestURI());

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", exception.getCodigo());
        corpo.put("mensagem", exception.getMessage());
        corpo.put("detalhes", detalhes);
        corpo.put("correlationId", correlationId);
        corpo.put("timestamp", Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_CORRELATION_ID, correlationId);
        return new ResponseEntity<>(corpo, headers, HttpStatus.CONFLICT);
    }
}
