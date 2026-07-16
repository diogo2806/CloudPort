package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoGuindaste;
import java.time.LocalDateTime;
import java.util.List;

public record PlanoGuindasteDTO(
        Long visitaNavioId,
        String berco,
        StatusPlanoGuindaste status,
        String usuario,
        String observacao,
        LocalDateTime atualizadoEm,
        List<AlocacaoGuindasteDTO> guindastes
) {
}
