package br.com.cloudport.servicoyard.vesselplanner.dto;

public class ViolacaoHardConstraintDto {

    private String tipo;
    private String descricao;
    private Long slotId;
    private String severidade;

    public ViolacaoHardConstraintDto() {
    }

    public ViolacaoHardConstraintDto(String tipo, String descricao, Long slotId, String severidade) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.slotId = slotId;
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

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getSeveridade() {
        return severidade;
    }

    public void setSeveridade(String severidade) {
        this.severidade = severidade;
    }
}
