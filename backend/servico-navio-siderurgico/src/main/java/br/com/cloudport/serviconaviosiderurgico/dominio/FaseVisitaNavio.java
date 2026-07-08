package br.com.cloudport.serviconaviosiderurgico.dominio;

public enum FaseVisitaNavio {
    PREVISTA,
    FUNDEADA,
    ATRACADA,
    OPERANDO,
    OPERACAO_CONCLUIDA,
    PARTIU,
    CANCELADA;

    public boolean terminal() {
        return this == PARTIU || this == CANCELADA;
    }

    public boolean permiteTransicaoPara(FaseVisitaNavio destino) {
        if (destino == null || this == destino || terminal()) {
            return false;
        }
        if (destino == CANCELADA) {
            return true;
        }
        return switch (this) {
            case PREVISTA -> destino == FUNDEADA || destino == ATRACADA;
            case FUNDEADA -> destino == ATRACADA;
            case ATRACADA -> destino == OPERANDO;
            case OPERANDO -> destino == OPERACAO_CONCLUIDA;
            case OPERACAO_CONCLUIDA -> destino == PARTIU;
            case PARTIU, CANCELADA -> false;
        };
    }
}
