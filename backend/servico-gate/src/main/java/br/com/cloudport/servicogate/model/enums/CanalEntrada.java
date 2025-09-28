package br.com.cloudport.servicogate.model.enums;

public enum CanalEntrada {
    PORTARIA_PRINCIPAL("Portaria principal"),
    BALANCA("Balança"),
    ALCANCE_REMOTO("Agendamento remoto"),
    APLICATIVO_MOTORISTA("Aplicativo do motorista"),
    INTEGRACAO_TOS("Integração TOS");

    private final String descricao;

    CanalEntrada(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
