package br.com.cloudport.visibilidade.exception;

import br.com.cloudport.visibilidade.dto.ErroApiDTO;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class VisibilidadeExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisibilidadeExceptionHandler.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroApiDTO> tratarRegraNegocio(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), request, ex, false);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroApiDTO> tratarValidacao(Exception ex,
                                                       HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS",
                "Os dados enviados nao atendem ao contrato da API.", request, ex, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApiDTO> tratarErroInesperado(Exception ex,
                                                            HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Nao foi possivel concluir a operacao.", request, ex, true);
    }

    private ResponseEntity<ErroApiDTO> resposta(HttpStatus status,
                                                 String codigo,
                                                 String mensagem,
                                                 HttpServletRequest request,
                                                 Exception ex,
                                                 boolean registrarStackTrace) {
        String correlationId = obterCorrelationId(request);
        String detalhes = request.getMethod() + " " + request.getRequestURI();
        ErroApiDTO erro = new ErroApiDTO(
                codigo,
                mensagem,
                detalhes,
                correlationId,
                LocalDateTime.now());

        if (registrarStackTrace) {
            LOGGER.error("Falha inesperada na API de visibilidade. correlationId={} rota={}",
                    correlationId, detalhes, ex);
        } else {
            LOGGER.warn("Requisicao rejeitada pela API de visibilidade. correlationId={} rota={} motivo={}",
                    correlationId, detalhes, ex.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(CORRELATION_HEADER, correlationId);
        return new ResponseEntity<>(erro, headers, status);
    }

    private String obterCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return correlationId.trim();
    }
}
