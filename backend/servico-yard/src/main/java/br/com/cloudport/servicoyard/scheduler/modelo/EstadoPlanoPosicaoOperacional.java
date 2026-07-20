package br.com.cloudport.servicoyard.scheduler.modelo;

public enum EstadoPlanoPosicaoOperacional {
    TENTATIVO,
    DEFINITIVO,
    IMINENTE,
    EXPIRADO,
    CANCELADO;

    public boolean permiteDispatchDireto() {
        return this == DEFINITIVO || this == IMINENTE;
    }

    public boolean ativo() {
        return this == TENTATIVO || this == DEFINITIVO || this == IMINENTE;
    }
}
