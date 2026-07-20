package br.com.cloudport.servicoyard.operacao2d;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class OperacaoGrafica2DDtos {

    private OperacaoGrafica2DDtos() {
    }

    @Schema(name = "RegistrarComandoOperacaoGrafica2DRequest")
    public record RegistrarComandoRequest(
            @NotBlank @Size(max = 120) String commandId,
            @NotBlank @Size(max = 80) String type,
            @Size(max = 1000) String reason,
            @NotNull JsonNode payload) {
    }

    @Schema(name = "ComandoOperacaoGrafica2DResponse")
    public record ComandoResponse(
            Long id,
            String commandId,
            String type,
            String status,
            String reason,
            JsonNode payload,
            String requestedBy,
            LocalDateTime createdAt) {
    }

    @Schema(name = "SalvarWorkspaceGrafico2DRequest")
    public record SalvarWorkspaceRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 20) String scope,
            @Size(max = 80) String role,
            @NotNull JsonNode content) {
    }

    @Schema(name = "WorkspaceGrafico2DResponse")
    public record WorkspaceResponse(
            Long id,
            String name,
            String scope,
            String role,
            String owner,
            Long version,
            JsonNode content,
            LocalDateTime createdAt) {
    }
}
