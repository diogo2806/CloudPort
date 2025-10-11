package br.com.cloudport.servicogate.model.enums;

public enum StatusAgendamento {
    PENDENTE("Pendente"),
    CONFIRMADO("Confirmado"),
    EM_ATENDIMENTO("Em atendimento"),
    EM_EXECUCAO("Em execução"),
    CONCLUIDO("Concluído"),
    COMPLETO("Completo"),
    NO_SHOW("No show"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
