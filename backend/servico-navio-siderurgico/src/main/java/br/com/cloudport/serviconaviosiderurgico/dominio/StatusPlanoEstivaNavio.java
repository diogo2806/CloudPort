package br.com.cloudport.serviconaviosiderurgico.dominio;

public enum StatusPlanoEstivaNavio {
    RASCUNHO,
    VALIDADO,
    EM_EXECUCAO,
    CONCLUIDO,
    INVALIDADO,
    CANCELADO;

    public boolean encerrado() {
        return this == CONCLUIDO || this == INVALIDADO || this == CANCELADO;
    }
}
