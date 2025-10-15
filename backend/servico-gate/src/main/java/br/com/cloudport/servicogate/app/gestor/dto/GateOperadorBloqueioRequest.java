package br.com.cloudport.servicogate.app.gestor.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record GateOperadorBloqueioRequest(@NotBlank @Size(max = 40) String motivoCodigo,
                                          @NotBlank @Size(max = 500) String justificativa,
                                          @Size(max = 40) String bloqueioAte) {
}
