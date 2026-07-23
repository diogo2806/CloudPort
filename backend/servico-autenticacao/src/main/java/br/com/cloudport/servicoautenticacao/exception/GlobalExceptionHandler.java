package br.com.cloudport.servicoautenticacao.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String HEADER = "X-Correlation-Id";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        String mensagem = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return resposta(status, status.name(), mensagem, null, request, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        String mensagem = ex.getMessage() == null || ex.getMessage().trim().isEmpty()
                ? "Acesso negado."
                : ex.getMessage();
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", mensagem, null, request, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estado(IllegalStateException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO", ex.getMessage(), null, request, ex);
    }

    @ExceptionHandler(PapelNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> papel(PapelNaoEncontradoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", ex.getMessage(), null, request, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumento(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), null, request, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String campo = "senha".equals(error.getField()) ? "password" : error.getField();
            if (!campos.containsKey(campo) || erroObrigatorio(error)) {
                campos.put(campo, error.getDefaultMessage());
            }
        }
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Erro de validação dos dados enviados.", campos, request, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS", "Erro de integridade dos dados.", null, request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex, HttpServletRequest request) {
        LOGGER.error("Erro interno no servico de autenticacao", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Nao foi possivel concluir a operacao.", null, request, ex);
    }

    private static boolean erroObrigatorio(FieldError error) {
        String codigo = error.getCode();
        return "NotBlank".equals(codigo) || "NotEmpty".equals(codigo) || "NotNull".equals(codigo);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String codigo, String mensagem,
                                                           Object campos, HttpServletRequest request, Exception ex) {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", request.getMethod() + " " + request.getRequestURI());
        if (campos != null) {
            detalhes.put("campos", campos);
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        corpo.put("detalhes", detalhes);
        if (campos != null) {
            corpo.put("erros", campos);
        }
        corpo.put("correlationId", correlationId);
        corpo.put("timestamp", Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER, correlationId);
        return new ResponseEntity<>(corpo, headers, status);
    }
}