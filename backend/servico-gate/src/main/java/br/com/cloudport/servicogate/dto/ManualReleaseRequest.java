package br.com.cloudport.servicogate.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ManualReleaseRequest {

    @NotNull
    private ManualReleaseAction acao;

    @Size(max = 40)
    private String motivo;

    @Size(max = 500)
    private String observacao;

    @Size(max = 80)
    private String operador;

    public ManualReleaseAction getAcao() {
        return acao;
    }

    public void setAcao(ManualReleaseAction acao) {
        this.acao = acao;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }
}
