package br.com.cloudport.servicogate.app.gestor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Size(max = 40)
    @Schema(description = "Identificação do chassis vinculado à visita", example = "CHASSIS-001")
    private String chassis;

    @Size(max = 20)
    @Schema(description = "Unidades transportadas que devem ficar exclusivas durante a visita")
    private List<String> unidades = new ArrayList<>();

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public String getChassis() { return chassis; }
    public void setChassis(String chassis) { this.chassis = chassis; }
    public List<String> getUnidades() { return unidades; }
    public void setUnidades(List<String> unidades) { this.unidades = unidades != null ? unidades : new ArrayList<>(); }
}
