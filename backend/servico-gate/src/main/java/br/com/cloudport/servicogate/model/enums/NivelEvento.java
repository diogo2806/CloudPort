package br.com.cloudport.servicogate.model.enums;

public enum NivelEvento {
    INFO("Informativo"),
    ALERTA("Alerta"),
    CRITICA("Crítica"),
    OPERACIONAL("Operacional");

    private final String descricao;

    NivelEvento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
