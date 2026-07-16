package br.com.cloudport.serviconavio.comum.erro;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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
    private static final String HEADER = "X-Correlation-Id";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(
                FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a, LinkedHashMap::new));
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Os dados enviados nao atendem ao contrato da API.", campos, request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> restricao(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> campos = ex.getConstraintViolations().stream().collect(Collectors.toMap(
                violacao -> violacao.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a, LinkedHashMap::new));
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Os parametros enviados nao atendem ao contrato da API.", campos, request, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumento(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), null, request, ex);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), "HTTP_" + ex.getRawStatusCode(), ex.getReason(), null, request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex, HttpServletRequest request) {
        LOGGER.error("Erro interno no servico de navio", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Nao foi possivel concluir a operacao.", null, request, ex);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String codigo, String mensagem,
                                                          Object campos, HttpServletRequest request, Exception ex) {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) correlationId = UUID.randomUUID().toString();
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", request.getMethod() + " " + request.getRequestURI());
        if (campos != null) detalhes.put("campos", campos);
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo); corpo.put("mensagem", mensagem); corpo.put("detalhes", detalhes);
        corpo.put("correlationId", correlationId); corpo.put("timestamp", Instant.now().toString());
        HttpHeaders headers = new HttpHeaders(); headers.set(HEADER, correlationId);
        return new ResponseEntity<>(corpo, headers, status);
    }
}
