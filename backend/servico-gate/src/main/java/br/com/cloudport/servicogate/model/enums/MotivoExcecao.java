package br.com.cloudport.servicogate.model.enums;

public enum MotivoExcecao {
    DOCUMENTACAO_INCOMPLETA("Documentação incompleta"),
    DIVERGENCIA_CARGA("Divergência de carga"),
    ATRASO_PROGRAMADO("Atraso programado"),
    FALHA_SEGURANCA("Falha de segurança"),
    OUTROS("Outros");

    private final String descricao;

    MotivoExcecao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
