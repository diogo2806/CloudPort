package br.com.cloudport.servicogate.app.gestor.dto;

import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "Dados necessários para registrar a entrada de uma pessoa no terminal")
public record EntradaPessoaRequest(
        @Schema(description = "Nome completo da pessoa", example = "Maria da Silva")
        @NotBlank @Size(max = 140) String nome,
        @Schema(description = "CPF, passaporte ou outro documento de identificação", example = "12345678900")
        @NotBlank @Size(max = 30) String documento,
        @Schema(description = "Vínculo operacional da pessoa com o terminal")
        @NotNull TipoPessoaAcesso tipoPessoa,
        @Schema(description = "Empresa, órgão ou embarcação vinculada")
        @Size(max = 140) String empresa,
        @Schema(description = "Número do crachá ou credencial")
        @Size(max = 50) String cracha,
        @Schema(description = "Portaria, catraca ou ponto onde a entrada foi registrada", example = "Portaria principal")
        @NotBlank @Size(max = 120) String pontoAcesso,
        @Schema(description = "Motivo da visita ou observação operacional")
        @Size(max = 500) String motivo,
        @Schema(hidden = true) @Size(max = 80) String origemAcao,
        @Schema(hidden = true) @Size(max = 100) String correlationId,
        @Schema(hidden = true) @Size(max = 120) String usuario) {
}
