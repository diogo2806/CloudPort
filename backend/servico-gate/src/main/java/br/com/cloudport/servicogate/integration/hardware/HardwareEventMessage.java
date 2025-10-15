package br.com.cloudport.servicogate.integration.hardware;

import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwareEventMessage {

    private String placa;
    private String qrCode;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String operador;
    private String origem;
    private String dispositivo;

    public GateFlowRequest toRequest() {
        GateFlowRequest request = new GateFlowRequest();
        request.setPlaca(Optional.ofNullable(placa).map(String::trim).orElse(null));
        request.setQrCode(Optional.ofNullable(qrCode).map(String::trim).orElse(null));
        request.setTimestamp(timestamp);
        request.setOperador(operador);
        return request;
    }

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

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
}
