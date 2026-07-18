package br.com.cloudport.servicoyard.vesselplanner.controlador;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.OptimisticLockException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = VesselPlannerControlador.class)
public class ExecucaoGuindasteConflitoHandler {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<Map<String, Object>> lockOtimista(
            Exception exception,
            HttpServletRequest request) {
        return conflito(
                "CONFLITO_VERSAO_EXECUCAO_GUINDASTE",
                "A execução foi alterada por outro operador. Recarregue os dados antes de continuar.",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridade(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return conflito(
                "CONFLITO_EXECUCAO_GUINDASTE",
                "A operação conflita com uma execução, ordem ou janela já persistida.",
                request);
    }

    private ResponseEntity<Map<String, Object>> conflito(
            String codigo,
            String mensagem,
            HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", request.getMethod() + " " + request.getRequestURI());

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        corpo.put("detalhes", detalhes);
        corpo.put("correlationId", correlationId);
        corpo.put("timestamp", Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set(CORRELATION_HEADER, correlationId);
        return new ResponseEntity<>(corpo, headers, HttpStatus.CONFLICT);
    }
}
