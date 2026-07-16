package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoGuindaste;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public record ComandoPlanoGuindasteDTO(
        @NotBlank(message = "Berco e obrigatorio.") String berco,
        @NotNull(message = "Status do plano e obrigatorio.") StatusPlanoGuindaste status,
        @NotEmpty(message = "Informe ao menos uma alocacao de guindaste.") List<@Valid AlocacaoGuindasteDTO> guindastes,
        String usuario,
        String observacao
) {
}
