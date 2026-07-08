package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoEstivaNavio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlanoEstivaNavioDTO(
        Long id,
        Long visitaNavioId,
        Integer versao,
        StatusPlanoEstivaNavio status,
        BigDecimal pesoTotalPlanejado,
        BigDecimal pesoTotalRealizado,
        LocalDateTime criadoEm,
        LocalDateTime validadoEm,
        List<PosicaoEstivaNavioDTO> posicoes
) {
    public static PlanoEstivaNavioDTO de(PlanoEstivaNavio plano, List<PosicaoEstivaNavioDTO> posicoes) {
        return new PlanoEstivaNavioDTO(
                plano.getId(),
                plano.getVisitaNavio().getId(),
                plano.getVersao(),
                plano.getStatus(),
                plano.getPesoTotalPlanejado(),
                plano.getPesoTotalRealizado(),
                plano.getCriadoEm(),
                plano.getValidadoEm(),
                posicoes
        );
    }
}
