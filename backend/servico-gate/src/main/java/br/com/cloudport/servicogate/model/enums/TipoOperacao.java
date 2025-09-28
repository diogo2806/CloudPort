package br.com.cloudport.servicogate.model.enums;

public enum TipoOperacao {
    ENTRADA("Entrada"),
    SAIDA("Saída"),
    DEVOLUCAO("Devolução"),
    TRANSFERENCIA("Transferência");

    private final String descricao;

    TipoOperacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
