package br.com.cloudport.servicogate.integration.tos;

import br.com.cloudport.servicogate.exception.BusinessException;

public class TosIntegrationException extends BusinessException {

    private final Integer statusHttp;
    private final String recurso;
    private final String identificadorMascarado;
    private final String codigoErro;
    private final String correlationId;

    public TosIntegrationException(String message) {
        this(message, null, "tos", "ausente", "TOS_ERRO", TosObservabilidadeSegura.obterCorrelationId(), null);
    }

    public TosIntegrationException(String message, Throwable cause) {
        this(message, null, "tos", "ausente", "TOS_ERRO", TosObservabilidadeSegura.obterCorrelationId(), cause);
    }

    public TosIntegrationException(String message,
                                   Integer statusHttp,
                                   String recurso,
                                   String identificadorMascarado,
                                   String codigoErro,
                                   String correlationId) {
        this(message, statusHttp, recurso, identificadorMascarado, codigoErro, correlationId, null);
    }

    public TosIntegrationException(String message,
                                   Integer statusHttp,
                                   String recurso,
                                   String identificadorMascarado,
                                   String codigoErro,
                                   String correlationId,
                                   Throwable cause) {
        super(message, cause);
        this.statusHttp = statusHttp;
        this.recurso = recurso;
        this.identificadorMascarado = identificadorMascarado;
        this.codigoErro = codigoErro;
        this.correlationId = correlationId;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getIdentificadorMascarado() {
        return identificadorMascarado;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
