package br.com.cloudport.servicogate.app.gestor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.Size;

public class GateFlowRequest {

    @Size(max = 10)
    private String placa;

    @Size(max = 120)
    private String qrCode;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Size(max = 80)
    private String operador;

    private UUID reservaCargaGeralId;

    private UUID commandIdCargaGeral;

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public UUID getReservaCargaGeralId() {
        return reservaCargaGeralId;
    }

    public void setReservaCargaGeralId(UUID reservaCargaGeralId) {
        this.reservaCargaGeralId = reservaCargaGeralId;
    }

    public UUID getCommandIdCargaGeral() {
        return commandIdCargaGeral;
    }

    public void setCommandIdCargaGeral(UUID commandIdCargaGeral) {
        this.commandIdCargaGeral = commandIdCargaGeral;
    }
}
