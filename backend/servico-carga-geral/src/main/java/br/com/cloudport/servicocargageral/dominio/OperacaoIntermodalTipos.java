package br.com.cloudport.servicocargageral.dominio;

public final class OperacaoIntermodalTipos {

    private OperacaoIntermodalTipos() {
    }

    public enum StatusPlanoOperacional {
        RASCUNHO,
        LIBERADO,
        EM_EXECUCAO,
        PARCIAL,
        CONCLUIDO,
        CANCELADO
    }

    public enum TipoUnidadeIntermodal {
        ARMAZEM,
        PATIO,
        CONTEINER,
        CAMINHAO,
        NAVIO,
        VAGAO,
        CLIENTE
    }

    public enum StatusReservaGate {
        RESERVADA,
        PARCIAL,
        CONFIRMADA,
        LIBERADA,
        CANCELADA
    }

    public enum StatusAvariaOperacional {
        ABERTA,
        SEGREGADA,
        EM_INSPECAO,
        EM_TRATAMENTO,
        REINTEGRADA,
        BAIXADA,
        BLOQUEADA,
        ENCERRADA
    }

    public enum ResultadoAvaria {
        REINTEGRAR,
        BAIXAR,
        MANTER_BLOQUEADA
    }

    public enum StatusInventarioFisico {
        ABERTO,
        EM_CONTAGEM,
        AGUARDANDO_APROVACAO,
        CONCLUIDO,
        CANCELADO
    }
}
