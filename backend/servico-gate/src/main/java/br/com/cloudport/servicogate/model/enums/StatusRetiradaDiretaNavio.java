package br.com.cloudport.servicogate.model.enums;

public enum StatusRetiradaDiretaNavio {
    FINALIZADA("Finalizada");

    private final String descricao;

    StatusRetiradaDiretaNavio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
