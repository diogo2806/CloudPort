package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusTransload;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class TransloadDTOs {

    private TransloadDTOs() {
    }

    @Schema(name = "ExecutarTransloadRequest", description = "Comando idempotente para transferir carga entre duas unidades canônicas")
    public record ExecutarTransloadRequest(
            @Schema(description = "Identificador idempotente do comando")
            @NotNull UUID commandId,
            @Schema(description = "Identificação canônica da unidade de origem", example = "CONT-ORIGEM-001")
            @NotBlank @Size(max = 80) String unidadeOrigem,
            @Schema(description = "Identificação canônica da unidade de destino", example = "CONT-DESTINO-001")
            @NotBlank @Size(max = 80) String unidadeDestino,
            @Schema(description = "Lacre conferido na unidade de origem")
            @Size(max = 80) String lacreOrigem,
            @Schema(description = "Lacre aplicado ou conferido na unidade de destino")
            @Size(max = 80) String lacreDestino,
            @Schema(description = "Divergência identificada durante a conferência")
            @Size(max = 1000) String divergencia,
            @Schema(description = "Código da avaria encontrada")
            @Size(max = 80) String codigoAvaria,
            @Schema(description = "Descrição da avaria encontrada")
            @Size(max = 1000) String descricaoAvaria,
            @Schema(description = "Usuário responsável pela execução")
            @NotBlank @Size(max = 120) String usuario,
            @Schema(description = "Correlação externa; quando ausente, o commandId é utilizado")
            @Size(max = 120) String correlationId,
            @Schema(description = "Lotes e saldos transferidos")
            @Valid @NotEmpty @Size(max = 200) List<ItemTransloadRequest> itens) {
    }

    @Schema(name = "ItemTransloadRequest", description = "Transferência de saldo entre um lote de origem e um lote de destino")
    public record ItemTransloadRequest(
            @NotNull UUID loteOrigemId,
            @NotNull UUID loteDestinoId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg) {
    }

    @Schema(name = "TransloadResposta", description = "Estado persistido e recuperável da operação de transload")
    public record TransloadResposta(
            UUID id,
            UUID commandId,
            String unidadeOrigem,
            String unidadeDestino,
            UUID reservaOrigemId,
            UUID reservaDestinoId,
            String lacreOrigem,
            String lacreDestino,
            String divergencia,
            String codigoAvaria,
            String descricaoAvaria,
            StatusTransload status,
            String motivoCancelamento,
            String usuario,
            String correlationId,
            OffsetDateTime executadoEm,
            List<ItemTransloadResposta> itens) {
    }

    @Schema(name = "ItemTransloadResposta")
    public record ItemTransloadResposta(
            UUID loteOrigemId,
            UUID loteDestinoId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg) {
    }
}
