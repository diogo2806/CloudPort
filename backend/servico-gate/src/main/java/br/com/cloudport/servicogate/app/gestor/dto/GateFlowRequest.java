package br.com.cloudport.servicogate.app.gestor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
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

    @Schema(description = "Indica que o fluxo pertence a uma sessão de troca de cavalo")
    private Boolean trocaCavalo = Boolean.FALSE;

    @Size(max = 14)
    @Schema(example = "123.456.789-00")
    private String cpfMotorista;

    @Size(max = 20)
    @Schema(example = "01234567890")
    private String numeroCnh;

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public Boolean getTrocaCavalo() { return trocaCavalo; }
    public void setTrocaCavalo(Boolean trocaCavalo) { this.trocaCavalo = trocaCavalo; }
    public String getCpfMotorista() { return cpfMotorista; }
    public void setCpfMotorista(String cpfMotorista) { this.cpfMotorista = cpfMotorista; }
    public String getNumeroCnh() { return numeroCnh; }
    public void setNumeroCnh(String numeroCnh) { this.numeroCnh = numeroCnh; }
}
