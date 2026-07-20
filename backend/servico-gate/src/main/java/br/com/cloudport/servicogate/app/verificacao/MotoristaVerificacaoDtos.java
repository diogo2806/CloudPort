package br.com.cloudport.servicogate.app.verificacao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public final class MotoristaVerificacaoDtos {

    private MotoristaVerificacaoDtos() {
    }

    public record CredencialMotoristaRequest(
            @NotBlank @Schema(allowableValues = {"PIN", "CREDENCIAL"}) String tipo,
            @NotBlank @Size(min = 4, max = 120) String valor,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record CredencialMotoristaDTO(
            Long id,
            Long motoristaId,
            Long transportadoraId,
            String tipo,
            String status,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim,
            String cadastradoPor,
            LocalDateTime cadastradoEm) {
    }

    public record VerificacaoMotoristaRequest(
            @NotBlank @Schema(allowableValues = {"PIN", "DOCUMENTO", "CREDENCIAL"}) String metodo,
            @NotBlank @Size(max = 120) String valor,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record OverrideVerificacaoMotoristaRequest(
            @NotBlank @Size(min = 10, max = 500) String motivo,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record VerificacaoMotoristaDTO(
            Long id,
            String chaveOperacional,
            Long truckVisitId,
            Long agendamentoId,
            Long motoristaId,
            String motorista,
            Long transportadoraId,
            String transportadora,
            String status,
            String metodo,
            int tentativas,
            int limiteTentativas,
            int tentativasRestantes,
            LocalDateTime bloqueadoAte,
            LocalDateTime verificadoEm,
            LocalDateTime expiraEm,
            String verificadoPor,
            String overridePor,
            String motivoOverride,
            String ultimoMotivo) {
    }
}
