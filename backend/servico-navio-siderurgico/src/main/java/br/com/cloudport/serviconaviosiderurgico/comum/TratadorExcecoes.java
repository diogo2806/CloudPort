package br.com.cloudport.serviconaviosiderurgico.comum;

import br.com.cloudport.contracts.api.ErroApi;
import br.com.cloudport.serviconaviosiderurgico.configuracao.CorrelationIdFilter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TratadorExcecoes {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErroApi> tratarArgumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO_NEGOCIO", ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroApi> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        erro -> erro.getField(),
                        erro -> Optional.ofNullable(erro.getDefaultMessage()).orElse("Valor invalido."),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO_ENTRADA", "Dados de entrada invalidos.", detalhes, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErroApi> tratarRestricao(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> detalhes = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violacao -> violacao.getPropertyPath().toString(),
                        violacao -> violacao.getMessage(),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO_ENTRADA", "Parametros invalidos.", detalhes, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroApi> tratarTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "PARAMETRO_INVALIDO",
                "Valor invalido para o parametro " + ex.getName() + ".",
                Map.of("parametro", ex.getName(), "valor", String.valueOf(ex.getValue())),
                request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErroApi> tratarStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        String codigo = switch (status) {
            case NOT_FOUND -> "RECURSO_NAO_ENCONTRADO";
            case CONFLICT -> "CONFLITO_NEGOCIO";
            case UNAUTHORIZED -> "NAO_AUTENTICADO";
            case FORBIDDEN -> "ACESSO_NEGADO";
            case SERVICE_UNAVAILABLE -> "INTEGRACAO_INDISPONIVEL";
            default -> "ERRO_HTTP_" + status.value();
        };
        String mensagem = Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase());
        return resposta(status, codigo, mensagem, Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroApi> tratarInesperado(Exception ex, HttpServletRequest request) {
        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERRO_INTERNO",
                "Ocorreu um erro interno ao processar a solicitacao.",
                Map.of(),
                request
        );
    }

    private ResponseEntity<ErroApi> resposta(HttpStatus status,
                                               String codigo,
                                               String mensagem,
                                               Map<String, Object> detalhes,
                                               HttpServletRequest request) {
        ErroApi erro = new ErroApi(
                codigo,
                mensagem == null || mensagem.isBlank() ? status.getReasonPhrase() : mensagem,
                detalhes,
                CorrelationIdFilter.obter(request),
                Instant.now()
        );
        return ResponseEntity.status(status).body(erro);
    }
}
