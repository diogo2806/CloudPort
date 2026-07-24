package br.com.cloudport.servicoyard.patio.avisoestivagem.modelo;

public final class AvisoEstivagemPatioEnums {

    private AvisoEstivagemPatioEnums() {
    }

    public enum StatusAvisoEstivagemPatio {
        ABERTO,
        ATRIBUIDO,
        EM_CORRECAO,
        AGUARDANDO_REVALIDACAO,
        RESOLVIDO,
        REABERTO
    }

    public enum TipoRegraEstivagemPatio {
        PESO,
        ALTURA,
        TIPO_CARGA,
        REEFER,
        PERIGOSO,
        CAPACIDADE,
        RESERVA,
        APOIO,
        REGRA_PILHA
    }

    public enum SeveridadeAvisoEstivagemPatio {
        CRITICA,
        ALTA,
        MEDIA,
        BAIXA
    }

    public enum TipoEventoHistoricoAvisoEstivagemPatio {
        ABERTURA,
        ATRIBUICAO,
        CORRECAO_INICIADA,
        ENVIO_REVALIDACAO,
        REVALIDACAO_FALHOU,
        RESOLUCAO,
        REABERTURA,
        ATUALIZACAO_AUTOMATICA
    }
}
