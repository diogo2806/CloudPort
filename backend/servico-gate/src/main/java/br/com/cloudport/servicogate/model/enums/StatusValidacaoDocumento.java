package br.com.cloudport.servicogate.model.enums;

public enum StatusValidacaoDocumento {

    PROCESSANDO("Processando validação..."),
    VALIDADO("Validado!"),
    FALHA("Falha na validação"),
    PENDENTE("Pendente de validação");

    private final String descricao;

    StatusValidacaoDocumento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
