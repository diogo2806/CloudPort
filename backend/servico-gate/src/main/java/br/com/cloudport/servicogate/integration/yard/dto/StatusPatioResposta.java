package br.com.cloudport.servicogate.integration.yard.dto;

public class StatusPatioResposta {

    private String status;
    private String descricao;
    private String verificadoEm;

    public StatusPatioResposta() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getVerificadoEm() {
        return verificadoEm;
    }

    public void setVerificadoEm(String verificadoEm) {
        this.verificadoEm = verificadoEm;
    }
}
