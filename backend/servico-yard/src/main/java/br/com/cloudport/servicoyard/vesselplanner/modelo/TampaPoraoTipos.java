package br.com.cloudport.servicoyard.vesselplanner.modelo;

public final class TampaPoraoTipos {

    private TampaPoraoTipos() {
    }

    public enum EstadoTampaPorao {
        FECHADA,
        ABERTA,
        REMOVIDA,
        POSICIONADA
    }

    public enum TipoOperacaoTampaPorao {
        ABRIR,
        REMOVER,
        POSICIONAR,
        FECHAR
    }

    public enum StatusTarefaTampaPorao {
        PLANEJADA,
        EM_EXECUCAO,
        CONCLUIDA,
        CANCELADA
    }

    public enum TipoPosicaoTampaPorao {
        SOBRE_PORAO,
        AREA_SEGURA,
        CONVES,
        CAIS,
        EQUIPAMENTO
    }
}
