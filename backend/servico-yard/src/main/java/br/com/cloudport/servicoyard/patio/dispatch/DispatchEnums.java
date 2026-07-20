package br.com.cloudport.servicoyard.patio.dispatch;

public final class DispatchEnums {

    private DispatchEnums() {
    }

    public enum ModoDispatch {
        MANUAL,
        SEMIAUTOMATICO,
        AUTOMATICO
    }

    public enum StatusConfiguracao {
        RASCUNHO,
        ATIVA,
        INATIVA
    }

    public enum TipoEscopo {
        TERMINAL,
        PATIO,
        BLOCO,
        POW,
        POOL,
        FILA
    }

    public enum TipoEtapa {
        DESLOCAMENTO_ORIGEM,
        CHEGADA_ORIGEM,
        COLETA,
        TRANSPORTE,
        ENTREGA,
        CONFIRMACAO_FISICA
    }

    public enum StatusEtapa {
        PENDENTE,
        EM_EXECUCAO,
        CONCLUIDA,
        FALHA,
        IGNORADA
    }

    public enum StatusDecisao {
        RECOMENDADA,
        ATRIBUIDA,
        REJEITADA,
        CANCELADA,
        CONCLUIDA,
        FALHA
    }

    public enum TipoAuxiliar {
        CHASSI,
        BOMB_CART,
        CASSETTE,
        ACESSORIO
    }

    public enum StatusReservaAuxiliar {
        RESERVADO,
        ASSOCIADO,
        DEVOLVIDO,
        CANCELADO
    }
}
