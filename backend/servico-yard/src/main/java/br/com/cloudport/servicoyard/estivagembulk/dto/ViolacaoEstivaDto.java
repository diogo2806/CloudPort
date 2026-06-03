package br.com.cloudport.servicoyard.estivagembulk.dto;

public class ViolacaoEstivaDto {

    private String tipo;
    private String descricao;
    private Long referenciaId;
    private String severidade;

    public ViolacaoEstivaDto() {
    }

    public ViolacaoEstivaDto(String tipo, String descricao, Long referenciaId, String severidade) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.referenciaId = referenciaId;
        this.severidade = severidade;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(Long referenciaId) {
        this.referenciaId = referenciaId;
    }

    public String getSeveridade() {
        return severidade;
    }

    public void setSeveridade(String severidade) {
        this.severidade = severidade;
    }
}
