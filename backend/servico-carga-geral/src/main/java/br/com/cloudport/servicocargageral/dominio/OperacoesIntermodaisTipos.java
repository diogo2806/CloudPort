package br.com.cloudport.servicocargageral.dominio;

public final class OperacoesIntermodaisTipos {

    private OperacoesIntermodaisTipos() {
    }

    public enum StatusTransload {
        CONCLUIDO,
        CANCELADO
    }

    public enum TipoMovimentoGateCarga {
        ENTREGA,
        RETIRADA
    }

    public enum EstagioGateCarga {
        ENTRADA,
        SAIDA
    }

    public enum StatusReservaGateCarga {
        RESERVADA,
        CONFIRMADA,
        COMPENSADA
    }

    public enum StatusAlocacaoCargoLot {
        PLANEJADA,
        RESERVADA,
        CONFIRMADA,
        CANCELADA
    }

    public enum ModalTransporteCargo {
        NAVIO,
        FERROVIA
    }

    public enum TipoOperacaoTransporteCargo {
        CARGA,
        DESCARGA
    }

    public enum StatusPlanoTransporteCargo {
        PLANEJADO,
        EM_EXECUCAO,
        CONCLUIDO,
        CANCELADO
    }

    public enum StatusAvariaCarga {
        ABERTA,
        BLOQUEADA,
        EM_INSPECAO,
        EM_REPARO,
        REPARADA,
        BAIXADA
    }

    public enum StatusInventarioFisicoCargo {
        ABERTO,
        EM_CONTAGEM,
        AGUARDANDO_APROVACAO,
        CONCLUIDO,
        CANCELADO
    }

    public enum StatusDivergenciaInventarioCargo {
        SEM_DIVERGENCIA,
        PENDENTE,
        AJUSTADA,
        REJEITADA
    }
}
