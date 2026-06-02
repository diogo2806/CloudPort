package br.com.cloudport.servicogate.app.gestor.dto;

public class GateEntradaPendenteBarcodeDTO {

    private Long gatePassId;
    private String tokenGatePass;
    private String codigo;
    private String containerNumber;
    private String placa;
    private String mensagem;

    public GateEntradaPendenteBarcodeDTO(Long gatePassId, String tokenGatePass, String codigo,
                                         String containerNumber, String placa) {
        this.gatePassId = gatePassId;
        this.tokenGatePass = tokenGatePass;
        this.codigo = codigo;
        this.containerNumber = containerNumber;
        this.placa = placa;
        this.mensagem = "Aguardando confirmação de barcode do operador DMT";
    }

    public Long getGatePassId() {
        return gatePassId;
    }

    public String getTokenGatePass() {
        return tokenGatePass;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getContainerNumber() {
        return containerNumber;
    }

    public String getPlaca() {
        return placa;
    }

    public String getMensagem() {
        return mensagem;
    }
}
