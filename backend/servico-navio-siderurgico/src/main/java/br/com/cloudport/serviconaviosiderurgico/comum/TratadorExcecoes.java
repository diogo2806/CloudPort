package br.com.cloudport.serviconaviosiderurgico.comum;

import br.com.cloudport.contracts.api.ErroApi;
import br.com.cloudport.serviconaviosiderurgico.configuracao.CorrelationIdFilter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TratadorExcecoes {

    private static final Logger LOGGER = LoggerFactory.getLogger(TratadorExcecoes.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroApi> tratarArgumentoInvalido(IllegalArgumentException ex,
                                                            HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO_NEGOCIO", ex.getMessage(), Map.of(), request, ex, false);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> tratarValidacao(MethodArgumentNotValidException ex,
                                                    HttpServletRequest request) {
        Map<String, Object> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        erro -> erro.getField(),
                        erro -> Optional.ofNullable(erro.getDefaultMessage()).orElse("Valor invalido."),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO_ENTRADA", "Dados de entrada invalidos.", detalhes,
                request, ex, false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroApi> tratarRestricao(ConstraintViolationException ex,
                                                    HttpServletRequest request) {
        Map<String, Object> detalhes = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violacao -> violacao.getPropertyPath().toString(),
                        violacao -> violacao.getMessage(),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO_ENTRADA", "Parametros invalidos.", detalhes,
                request, ex, false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroApi> tratarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "PARAMETRO_INVALIDO",
                "Valor invalido para o parametro " + ex.getName() + ".",
                Map.of("parametro", ex.getName(), "valor", String.valueOf(ex.getValue())),
                request,
                ex,
                false
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroApi> tratarStatus(ResponseStatusException ex,
                                                 HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        String codigo = switch (status) {
            case NOT_FOUND -> "RECURSO_NAO_ENCONTRADO";
            case CONFLICT -> "CONFLITO_NEGOCIO";
            case UNAUTHORIZED -> "NAO_AUTENTICADO";
            case FORBIDDEN -> "ACESSO_NEGADO";
            case UNPROCESSABLE_ENTITY -> "MENSAGEM_REJEITADA";
            case SERVICE_UNAVAILABLE -> "INTEGRACAO_INDISPONIVEL";
            default -> "ERRO_HTTP_" + status.value();
        };
        String mensagem = Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase());
        return resposta(status, codigo, mensagem, Map.of(), request, ex, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> tratarInesperado(Exception ex, HttpServletRequest request) {
        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERRO_INTERNO",
                "Ocorreu um erro interno ao processar a solicitacao.",
                Map.of(),
                request,
                ex,
                true
        );
    }

    private ResponseEntity<ErroApi> resposta(HttpStatus status,
                                              String codigo,
                                              String mensagem,
                                              Map<String, Object> detalhesAdicionais,
                                              HttpServletRequest request,
                                              Exception ex,
                                              boolean stackTrace) {
        String correlationId = correlationId(request);
        String rota = request.getMethod() + " " + request.getRequestURI();
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("rota", rota);
        detalhes.putAll(detalhesAdicionais);
        ErroApi erro = new ErroApi(
                codigo,
                mensagem == null || mensagem.isBlank() ? status.getReasonPhrase() : mensagem,
                detalhes,
                correlationId,
                Instant.now()
        );
        if (stackTrace) {
            LOGGER.error("Falha inesperada. correlationId={} rota={}", correlationId, rota, ex);
        } else {
            LOGGER.warn("Requisicao rejeitada. correlationId={} rota={} motivo={}",
                    correlationId, rota, ex.getMessage());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdFilter.HEADER, correlationId);
        return new ResponseEntity<>(erro, headers, status);
    }

    private String correlationId(HttpServletRequest request) {
        String correlationId = CorrelationIdFilter.obter(request);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        String header = request.getHeader(CorrelationIdFilter.HEADER);
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
    }
}
