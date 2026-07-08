package br.com.cloudport.serviconaviosiderurgico.dominio;

public enum StatusPlanoEstivaNavio {
    RASCUNHO,
    VALIDADO,
    EM_EXECUCAO,
    CONCLUIDO,
    CANCELADO;

    public boolean encerrado() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
