package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.enumeracao.StatusServicoPatioEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusPatioDto {

    @Schema(description = "Status atual do serviço de pátio", required = true, example = "DISPONIVEL")
    private StatusServicoPatioEnum status;

    @Schema(description = "Descrição amigável do status do serviço", required = true,
            example = "Serviço de pátio operacional.")
    private String descricao;

    @Schema(description = "Instante da última verificação", required = true, type = "string", format = "date-time")
    private LocalDateTime verificadoEm;
}
