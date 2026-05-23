package br.com.cloudport.serviconavio.escala.entidade;

import java.util.Set;

/**
 * Fases de uma escala (vessel visit) inspiradas nas visit phases do Navis N4:
 * Created -> Inbound -> Arrived/Working -> Departed -> Closed (e Canceled).
 */
public enum FaseEscala {
    PREVISTA,
    INBOUND,
    ATRACADO,
    OPERANDO,
    PARTIU,
    ENCERRADA,
    CANCELADA;

    private static Set<FaseEscala> transicoesPermitidas(FaseEscala fase) {
        switch (fase) {
            case PREVISTA:
                return Set.of(INBOUND, ATRACADO, CANCELADA);
            case INBOUND:
                return Set.of(ATRACADO, CANCELADA);
            case ATRACADO:
                return Set.of(OPERANDO, PARTIU, CANCELADA);
            case OPERANDO:
                return Set.of(PARTIU);
            case PARTIU:
                return Set.of(ENCERRADA);
            case ENCERRADA:
            case CANCELADA:
            default:
                return Set.of();
        }
    }

    public boolean podeTransicionarPara(FaseEscala destino) {
        return destino != null && transicoesPermitidas(this).contains(destino);
    }

    public boolean isTerminal() {
        return this == ENCERRADA || this == CANCELADA;
    }
}
