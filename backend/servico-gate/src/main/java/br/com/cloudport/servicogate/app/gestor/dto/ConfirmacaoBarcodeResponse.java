package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;

public class ConfirmacaoBarcodeResponse {

    private Long gatePassId;
    private String tokenGatePass;
    private String codigoBarcode;
    private String statusConfirmacao;
    private LocalDateTime dataConfirmacao;
    private String mensagem;

    public ConfirmacaoBarcodeResponse() {
    }

    public ConfirmacaoBarcodeResponse(Long gatePassId, String tokenGatePass, String codigoBarcode,
                                      String statusConfirmacao, LocalDateTime dataConfirmacao, String mensagem) {
        this.gatePassId = gatePassId;
        this.tokenGatePass = tokenGatePass;
        this.codigoBarcode = codigoBarcode;
        this.statusConfirmacao = statusConfirmacao;
        this.dataConfirmacao = dataConfirmacao;
        this.mensagem = mensagem;
    }

    public Long getGatePassId() {
        return gatePassId;
    }

    public void setGatePassId(Long gatePassId) {
        this.gatePassId = gatePassId;
    }

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

    public String getStatusConfirmacao() {
        return statusConfirmacao;
    }

    public void setStatusConfirmacao(String statusConfirmacao) {
        this.statusConfirmacao = statusConfirmacao;
    }

    public LocalDateTime getDataConfirmacao() {
        return dataConfirmacao;
    }

    public void setDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
