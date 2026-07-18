package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class EventoVmtWorkInstructionRequest {

    @NotBlank
    @Size(max = 120)
    private String eventId;

    @NotNull
    private TipoEventoVmt tipoEvento;

    @NotNull
    private StatusConfirmacaoVmt statusEsperado;

    @NotNull
    private LocalDateTime timestamp;

    @Size(max = 1000)
    private String resultado;

    private String payload;

    @Size(max = 120)
    private String operador;

    @Size(max = 120)
    private String correlationId;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public TipoEventoVmt getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoVmt tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusConfirmacaoVmt getStatusEsperado() { return statusEsperado; }
    public void setStatusEsperado(StatusConfirmacaoVmt statusEsperado) { this.statusEsperado = statusEsperado; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
