package br.com.cloudport.servicoautenticacao.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        logger.warn("IllegalStateException encontrada no GlobalExceptionHandler: ", ex);
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PapelNaoEncontradoException.class)
    public ResponseEntity<Object> handlePapelNaoEncontradoException(PapelNaoEncontradoException ex) {
        logger.warn("PapelNaoEncontradoException encontrada no GlobalExceptionHandler: ", ex);
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("IllegalArgumentException encontrada no GlobalExceptionHandler: ", ex);
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.warn("Erro de validação encontrado no GlobalExceptionHandler: ", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensagem", "Erro de validação dos dados enviados.");

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String fieldName = normalizarNomeCampo(error.getField());
            if (!fieldErrors.containsKey(fieldName) || isNotBlankError(error)) {
                fieldErrors.put(fieldName, error.getDefaultMessage());
            }
        }

        body.put("erros", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        logger.warn("DataIntegrityViolationException encontrada no GlobalExceptionHandler: ", ex);
        return new ResponseEntity<>("Erro de integridade dos dados", HttpStatus.CONFLICT);
    }

    private String normalizarNomeCampo(String fieldName) {
        if ("senha".equals(fieldName)) {
            return "password";
        }
        return fieldName;
    }

    private boolean isNotBlankError(FieldError error) {
        return error.getCode() != null && error.getCode().startsWith("NotBlank");
    }

    // outros manipuladores de exceções...
}
