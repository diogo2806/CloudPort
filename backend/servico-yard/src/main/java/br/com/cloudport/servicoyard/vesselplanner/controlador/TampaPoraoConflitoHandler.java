package br.com.cloudport.servicoyard.vesselplanner.controlador;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TampaPoraoControlador.class)
public class TampaPoraoConflitoHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(
            EntityNotFoundException exception,
            HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "TAMPA_PORAO_NAO_ENCONTRADA", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> requisicaoInvalida(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "OPERACAO_TAMPA_INVALIDA", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflitoOperacional(
            IllegalStateException exception,
            HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO_OPERACAO_TAMPA", exception.getMessage(), request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<Map<String, Object>> conflitoConcorrencia(
            Exception exception,
            HttpServletRequest request) {
        return resposta(
                HttpStatus.CONFLICT,
                "CONFLITO_VERSAO_TAMPA_PORAO",
                "A operação da tampa foi alterada por outro operador. Recarregue os dados antes de continuar.",
                request);
    }

    private ResponseEntity<Map<String, Object>> resposta(
            HttpStatus status,
            String codigo,
            String mensagem,
            HttpServletRequest request) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        corpo.put("rota", request.getMethod() + " " + request.getRequestURI());
        corpo.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(corpo);
    }
}
