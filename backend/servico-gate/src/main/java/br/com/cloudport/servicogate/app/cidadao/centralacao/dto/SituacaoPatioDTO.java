package br.com.cloudport.servicogate.app.cidadao.centralacao.dto;

public class SituacaoPatioDTO {

    private String status;
    private String descricao;
    private String verificadoEm;

    public SituacaoPatioDTO() {
    }

    public SituacaoPatioDTO(String status, String descricao, String verificadoEm) {
        this.status = status;
        this.descricao = descricao;
        this.verificadoEm = verificadoEm;
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
