package br.com.cloudport.servicocargageral.dominio;

public final class CargaGeralTipos {

    private CargaGeralTipos() {
    }

    public enum TipoOperacaoConhecimento {
        IMPORTACAO,
        EXPORTACAO,
        CABOTAGEM,
        TRANSBORDO
    }

    public enum StatusConhecimentoCarga {
        RASCUNHO,
        MANIFESTADO,
        EM_OPERACAO,
        CONCLUIDO,
        CANCELADO
    }

    public enum NaturezaCarga {
        CARGA_SOLTA,
        CARGA_PROJETO,
        BREAK_BULK
    }

    public enum StatusLoteCarga {
        PROGRAMADO,
        NO_TERMINAL,
        PARCIALMENTE_CARREGADO,
        PARCIALMENTE_DESCARREGADO,
        AVARIADO,
        BLOQUEADO,
        CONCLUIDO,
        CANCELADO
    }

    public enum TipoMovimentacaoCarga {
        RECEBIMENTO,
        DESCARGA_PARCIAL,
        ARMAZENAGEM,
        TRANSFERENCIA,
        CARGA_PARCIAL,
        ENTREGA,
        CONSOLIDACAO,
        DESCONSOLIDACAO,
        AJUSTE_INVENTARIO
    }

    public enum TipoOperacaoStuffUnstuff {
        STUFF,
        UNSTUFF
    }

    public enum StatusOperacaoStuffUnstuff {
        PLANEJADA,
        EM_EXECUCAO,
        PARCIAL,
        CONCLUIDA,
        CANCELADA
    }

    public enum TipoEventoStuffUnstuff {
        CRIADA,
        INICIADA,
        EXECUCAO_REGISTRADA,
        DIVERGENCIA_REGISTRADA,
        AVARIA_REGISTRADA,
        CONCLUIDA,
        CANCELADA
    }

    public enum TipoPlanoIntermodalCarga {
        STUFF,
        UNSTUFF,
        TRANSLOAD,
        RETIRADA_GATE,
        ENTREGA_GATE,
        MOVIMENTACAO_PATIO,
        CARGA_NAVIO,
        DESCARGA_NAVIO,
        CARGA_FERROVIARIA,
        DESCARGA_FERROVIARIA,
        INSPECAO_AVARIA,
        INVENTARIO
    }

    public enum StatusPlanoIntermodalCarga {
        PLANEJADA,
        LIBERADA,
        EM_EXECUCAO,
        CONCLUIDA,
        CANCELADA
    }

    public enum PrioridadePlanoIntermodalCarga {
        BAIXA,
        NORMAL,
        ALTA,
        URGENTE
    }

    public enum TipoEventoPlanoIntermodalCarga {
        CRIADA,
        NOVA_VERSAO,
        LIBERADA,
        RECURSO_ATRIBUIDO,
        INICIADA,
        EXECUCAO_REGISTRADA,
        DIVERGENCIA_REGISTRADA,
        AVARIA_REGISTRADA,
        RECONCILIADA,
        CONCLUIDA,
        CANCELADA,
        COMPENSADA
    }

    public enum TipoReservaGateCarga {
        RETIRADA,
        ENTREGA
    }

    public enum StatusReservaGateCarga {
        RESERVADA,
        CONFIRMADA,
        LIBERADA,
        CANCELADA
    }

    public enum StatusAvariaCarga {
        ABERTA,
        SEGREGADA,
        EM_INSPECAO,
        EM_REPARO,
        REPARADA,
        BAIXADA,
        ENCERRADA
    }

    public enum ResultadoTratamentoAvaria {
        REINTEGRAR,
        BAIXAR,
        MANTER_BLOQUEADA
    }

    public enum TipoEventoAvariaCarga {
        REGISTRADA,
        SEGREGADA,
        INSPECAO_INICIADA,
        INSPECAO_REGISTRADA,
        REINTEGRADA,
        BAIXADA,
        MANTIDA_BLOQUEADA
    }

    public enum StatusInventarioFisicoCarga {
        ABERTO,
        EM_CONTAGEM,
        AGUARDANDO_APROVACAO,
        CONCLUIDO,
        CANCELADO
    }

    public enum CategoriaReferenciaCarga {
        COMMODITY,
        TIPO_EMBALAGEM,
        TIPO_PRODUTO,
        CODIGO_ARMAZENAGEM,
        CODIGO_MANUSEIO,
        MERCADORIA_PERIGOSA,
        FAIXA_TEMPERATURA,
        TIPO_AVARIA
    }
}
