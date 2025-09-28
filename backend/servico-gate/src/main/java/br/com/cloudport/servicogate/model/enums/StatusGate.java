package br.com.cloudport.servicogate.model.enums;

public enum StatusGate {
    AGUARDANDO_ENTRADA("Aguardando entrada"),
    EM_PROCESSAMENTO("Em processamento"),
    LIBERADO("Liberado"),
    RETIDO("Retido"),
    FINALIZADO("Finalizado");

    private final String descricao;

    StatusGate(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
