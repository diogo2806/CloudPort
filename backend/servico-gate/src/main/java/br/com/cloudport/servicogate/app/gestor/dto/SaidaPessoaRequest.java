package br.com.cloudport.servicogate.app.gestor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "Dados necessários para registrar a saída de uma pessoa do terminal")
public record SaidaPessoaRequest(
        @Schema(description = "CPF, passaporte ou outro documento de identificação", example = "12345678900")
        @NotBlank @Size(max = 30) String documento,
        @Schema(description = "Portaria, catraca ou ponto onde a saída foi registrada", example = "Portaria principal")
        @NotBlank @Size(max = 120) String pontoAcesso,
        @Schema(description = "Observação operacional da saída")
        @Size(max = 500) String motivo,
        @Schema(hidden = true) @Size(max = 80) String origemAcao,
        @Schema(hidden = true) @Size(max = 100) String correlationId,
        @Schema(hidden = true) @Size(max = 120) String usuario) {
}
