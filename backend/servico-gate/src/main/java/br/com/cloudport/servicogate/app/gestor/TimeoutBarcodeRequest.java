package br.com.cloudport.servicogate.app.gestor;

import javax.validation.constraints.NotBlank;

public class TimeoutBarcodeRequest {

    @NotBlank(message = "Token do gate pass é obrigatório")
    private String tokenGatePass;

    @NotBlank(message = "ID do dispositivo DMT é obrigatório")
    private String dispositivoDmtId;

    public String getTokenGatePass() {
        return tokenGatePass;
    }

    public void setTokenGatePass(String tokenGatePass) {
        this.tokenGatePass = tokenGatePass;
    }

    public String getDispositivoDmtId() {
        return dispositivoDmtId;
    }

    public void setDispositivoDmtId(String dispositivoDmtId) {
        this.dispositivoDmtId = dispositivoDmtId;
    }
}
