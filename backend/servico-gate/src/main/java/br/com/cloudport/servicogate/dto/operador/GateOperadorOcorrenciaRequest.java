package br.com.cloudport.servicogate.dto.operador;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record GateOperadorOcorrenciaRequest(@NotBlank @Size(max = 40) String tipoCodigo,
                                            @NotBlank @Size(max = 500) String descricao,
                                            Long veiculoId) {
}
