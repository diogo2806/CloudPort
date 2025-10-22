package br.com.cloudport.serviconavio.comum.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TratadorExcecoes {

    private static final Logger LOGGER = LoggerFactory.getLogger(TratadorExcecoes.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        LOGGER.warn("Erro de validação identificado", ex);
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("mensagem", "Há erros de validação nos dados enviados.");
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (primeiro, segundo) -> primeiro, LinkedHashMap::new));
        corpo.put("erros", erros);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarViolacoes(ConstraintViolationException ex) {
        LOGGER.warn("Violação de restrição identificada", ex);
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("mensagem", "Há erros de validação nos parâmetros informados.");
        Map<String, String> erros = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(ConstraintViolation::getPropertyPath, ConstraintViolation::getMessage, (primeiro, segundo) -> primeiro, LinkedHashMap::new));
        corpo.put("erros", erros);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarArgumentoInvalido(IllegalArgumentException ex) {
        LOGGER.warn("Argumento inválido recebido", ex);
        Map<String, String> corpo = new LinkedHashMap<>();
        corpo.put("mensagem", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarRespostaComStatus(ResponseStatusException ex) {
        LOGGER.warn("Resposta com status específico lançada", ex);
        Map<String, String> corpo = new LinkedHashMap<>();
        corpo.put("mensagem", ex.getReason());
        return ResponseEntity.status(ex.getStatus()).body(corpo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> tratarErroNaoEsperado(Exception ex) {
        LOGGER.error("Erro interno no serviço de navio", ex);
        Map<String, String> corpo = new LinkedHashMap<>();
        corpo.put("mensagem", "Erro interno ao processar a solicitação.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(corpo);
    }
}
