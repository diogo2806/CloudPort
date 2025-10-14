package br.com.cloudport.servicogate.dto.operador;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record GateOperadorLiberacaoRequest(@NotBlank @Size(max = 40) String canalEntrada,
                                           @NotBlank @Size(max = 500) String justificativa,
                                           Boolean notificarTransportadora) {
}
