package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ConfirmacaoBarcodeRequest {

    @NotBlank(message = "Token do gate pass é obrigatório")
    private String tokenGatePass;

    @NotBlank(message = "Código do barcode é obrigatório")
    private String codigoBarcode;

    @NotNull(message = "Status de confirmação é obrigatório")
    private Boolean confirmado;

    private String motivo;

    private LocalDateTime dataConfirmacao;

    @NotBlank(message = "ID do dispositivo DMT é obrigatório")
    private String dispositivoDmtId;

    public String getTokenGatePass() {
        return tokenGatePass;
    }

    public void setTokenGatePass(String tokenGatePass) {
        this.tokenGatePass = tokenGatePass;
    }

    public String getCodigoBarcode() {
        return codigoBarcode;
    }

    public void setCodigoBarcode(String codigoBarcode) {
        this.codigoBarcode = codigoBarcode;
    }

    public Boolean getConfirmado() {
        return confirmado;
    }

    public void setConfirmado(Boolean confirmado) {
        this.confirmado = confirmado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getDataConfirmacao() {
        return dataConfirmacao;
    }

    public void setDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
    }

    public String getDispositivoDmtId() {
        return dispositivoDmtId;
    }

    public void setDispositivoDmtId(String dispositivoDmtId) {
        this.dispositivoDmtId = dispositivoDmtId;
    }
}
