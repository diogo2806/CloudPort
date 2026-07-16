package br.com.cloudport.serviconaviosiderurgico.comum;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TratadorExcecoes {

    private static final Logger LOGGER = LoggerFactory.getLogger(TratadorExcecoes.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (primeiro, segundo) -> primeiro, LinkedHashMap::new));
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS",
                "Os dados enviados nao atendem ao contrato da API.", campos, request, ex, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarArgumentoInvalido(IllegalArgumentException ex,
                                                                       HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), null, request, ex, false);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> tratarStatus(ResponseStatusException ex,
                                                             HttpServletRequest request) {
        return resposta(ex.getStatus(), "HTTP_" + ex.getRawStatusCode(), ex.getReason(), null, request, ex, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErro(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Nao foi possivel concluir a operacao.", null, request, ex, true);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status,
                                                          String codigo,
                                                          String mensagem,
                                                          Object detalhesAdicionais,
                                                          HttpServletRequest request,
                                                          Exception ex,
                                                          boolean stackTrace) {
        String correlationId = correlationId(request);
        String rota = request.getMethod() + " " + request.getRequestURI();
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", rota);
        if (detalhesAdicionais != null) detalhes.put("campos", detalhesAdicionais);
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem == null ? status.getReasonPhrase() : mensagem);
        corpo.put("detalhes", detalhes);
        corpo.put("correlationId", correlationId);
        corpo.put("timestamp", Instant.now().toString());
        if (stackTrace) LOGGER.error("Falha inesperada. correlationId={} rota={}", correlationId, rota, ex);
        else LOGGER.warn("Requisicao rejeitada. correlationId={} rota={} motivo={}", correlationId, rota, ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.set(CORRELATION_HEADER, correlationId);
        return new ResponseEntity<>(corpo, headers, status);
    }

    private String correlationId(HttpServletRequest request) {
        String value = request.getHeader(CORRELATION_HEADER);
        return value == null || value.trim().isEmpty() ? UUID.randomUUID().toString() : value.trim();
    }
}
