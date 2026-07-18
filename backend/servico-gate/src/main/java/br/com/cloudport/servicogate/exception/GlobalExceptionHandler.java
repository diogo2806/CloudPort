package br.com.cloudport.servicogate.exception;

import br.com.cloudport.servicogate.integration.tos.TosIntegrationException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage(), Collections.emptyList());
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ApiError apiError = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage(), Collections.emptyList());
        if (!(ex instanceof TosIntegrationException)) {
            return new ResponseEntity<>(apiError, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        TosIntegrationException tosException = (TosIntegrationException) ex;
        String correlationId = resolverCorrelationId(tosException.getCorrelationId(), request);
        LOGGER.warn("event=tos.integration.handled resource={} identifier={} status={} errorCode={} correlationId={}",
                valorSeguro(tosException.getRecurso()),
                valorSeguro(tosException.getIdentificadorMascarado()),
                tosException.getStatusHttp() == null ? "unknown" : tosException.getStatusHttp(),
                valorSeguro(tosException.getCodigoErro()),
                correlationId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_CORRELATION_ID, correlationId);
        return new ResponseEntity<>(apiError, headers, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Erro de validação", errors);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatus status,
                                                                   WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        List<String> errors = bindingResult.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Erro de validação", errors);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String resolverCorrelationId(String correlationIdExcecao, HttpServletRequest request) {
        String correlationId = correlationIdExcecao;
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader(HEADER_CORRELATION_ID);
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

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "unknown" : valor;
    }
}
