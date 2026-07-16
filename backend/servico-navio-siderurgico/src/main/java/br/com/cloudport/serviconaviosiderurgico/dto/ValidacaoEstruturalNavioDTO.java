package br.com.cloudport.serviconaviosiderurgico.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ValidacaoEstruturalNavioDTO(
        Long visitaNavioId,
        Long planoEstivaId,
        LocalDateTime validadoEm,
        String status,
        List<OcorrenciaEstruturalDTO> erros,
        List<OcorrenciaEstruturalDTO> alertas,
        List<String> verificacoesNaoConfiguradas
) {
    public record OcorrenciaEstruturalDTO(
            String categoria,
            String codigo,
            String mensagem,
            Long itemOperacaoNavioId,
            Integer porao,
            Integer camada,
            Integer coluna
    ) {
    }
}
