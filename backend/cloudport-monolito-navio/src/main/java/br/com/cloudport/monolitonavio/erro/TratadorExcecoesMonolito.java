package br.com.cloudport.monolitonavio.erro;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TratadorExcecoesMonolito {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroApi> tratarStatus(ResponseStatusException ex,
                                                 HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        String mensagem = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return resposta(status, "REGRA_NEGOCIO", mensagem, request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> tratarValidacao(MethodArgumentNotValidException ex,
                                                    HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getAllErrors().stream()
                .map(erro -> {
                    String campo = erro instanceof FieldError
                            ? ((FieldError) erro).getField()
                            : erro.getObjectName();
                    return campo + ": " + Objects.toString(erro.getDefaultMessage(), "valor inválido");
                })
                .distinct()
                .collect(Collectors.toList());
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados de entrada inválidos.", request, detalhes);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroApi> tratarRestricao(ConstraintViolationException ex,
                                                    HttpServletRequest request) {
        List<String> detalhes = ex.getConstraintViolations().stream()
                .map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
                .sorted()
                .collect(Collectors.toList());
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados de entrada inválidos.", request, detalhes);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroApi> tratarArgumento(IllegalArgumentException ex,
                                                   HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroApi> tratarIntegridade(DataIntegrityViolationException ex,
                                                     HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DADOS",
                "A operação viola uma restrição de integridade.", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroApi> tratarAutenticacao(AuthenticationException ex,
                                                      HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                "Autenticação necessária ou credenciais inválidas.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApi> tratarAcesso(AccessDeniedException ex,
                                                HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "O usuário não possui permissão para esta operação.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> tratarInesperado(Exception ex,
                                                    HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro interno ao processar a solicitação.", request, List.of());
    }

    private ResponseEntity<ErroApi> resposta(HttpStatus status,
                                             String codigo,
                                             String mensagem,
                                             HttpServletRequest request,
                                             List<String> detalhes) {
        ErroApi erro = new ErroApi(
                OffsetDateTime.now(),
                status.value(),
                codigo,
                mensagem == null ? status.getReasonPhrase() : mensagem,
                request.getRequestURI(),
                MDC.get("correlationId"),
                detalhes);
        return ResponseEntity.status(status).body(erro);
    }
}
