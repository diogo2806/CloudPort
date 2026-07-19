package br.com.cloudport.servicogate.comum.erro;

import br.com.cloudport.servicogate.integration.tos.TosIntegrationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Os dados enviados nao atendem ao contrato da API.", campos, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumento(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", ex.getMessage(), null, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), "HTTP_" + ex.getRawStatusCode(), ex.getReason(), null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex, HttpServletRequest request) {
        TosIntegrationException tosException = localizarFalhaTos(ex);
        String correlationId = tosException == null
                ? resolverCorrelationId(null, request)
                : resolverCorrelationId(tosException.getCorrelationId(), request);
        if (tosException != null) {
            LOGGER.error("event=tos.integration.unhandled resource={} identifier={} status={} errorCode={} correlationId={}",
                    valorSeguro(tosException.getRecurso()),
                    valorSeguro(tosException.getIdentificadorMascarado()),
                    tosException.getStatusHttp() == null ? "unknown" : tosException.getStatusHttp(),
                    valorSeguro(tosException.getCodigoErro()),
                    correlationId);
        } else {
            LOGGER.error("event=gate.internal.error correlationId={}", correlationId, ex);
        }
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Nao foi possivel concluir a operacao.",
                null, request, correlationId);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status,
                                                           String codigo,
                                                           String mensagem,
                                                           Object campos,
                                                           HttpServletRequest request) {
        return resposta(status, codigo, mensagem, campos, request, resolverCorrelationId(null, request));
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status,
                                                           String codigo,
                                                           String mensagem,
                                                           Object campos,
                                                           HttpServletRequest request,
                                                           String correlationId) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", request.getMethod() + " " + request.getRequestURI());
        if (campos != null) {
            detalhes.put("campos", campos);
        }
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        corpo.put("detalhes", detalhes);
        corpo.put("correlationId", correlationId);
        corpo.put("timestamp", Instant.now().toString());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER, correlationId);
        return new ResponseEntity<>(corpo, headers, status);
    }

    private String resolverCorrelationId(String correlationIdExcecao, HttpServletRequest request) {
        String correlationId = correlationIdExcecao;
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader(HEADER);
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        String normalizado = correlationId.trim().replaceAll("[^A-Za-z0-9._:-]", "");
        if (normalizado.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return normalizado.length() <= 128 ? normalizado : normalizado.substring(0, 128);
    }

    private TosIntegrationException localizarFalhaTos(Throwable throwable) {
        Throwable atual = throwable;
        int profundidade = 0;
        while (atual != null && profundidade < 10) {
            if (atual instanceof TosIntegrationException) {
                return (TosIntegrationException) atual;
            }
            atual = atual.getCause();
            profundidade++;
        }
        return null;
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "unknown" : valor;
    }
}
