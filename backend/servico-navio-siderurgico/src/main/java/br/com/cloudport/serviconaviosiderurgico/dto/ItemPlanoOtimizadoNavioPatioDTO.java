package br.com.cloudport.serviconaviosiderurgico.dto;

public record ItemPlanoOtimizadoNavioPatioDTO(
        Long itemOperacaoNavioId,
        Long ordemTrabalhoPatioId,
        String codigoCarga,
        Integer linha,
        Integer coluna,
        String camada,
        String equipamento,
        Integer sequenciaPlano
) {
}
