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
