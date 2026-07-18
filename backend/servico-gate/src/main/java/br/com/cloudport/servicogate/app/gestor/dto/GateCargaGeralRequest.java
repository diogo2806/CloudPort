package br.com.cloudport.servicogate.app.gestor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "Dados para reserva de retirada ou entrega parcial de carga geral")
public class GateCargaGeralRequest {

    @NotNull
    @Schema(description = "Chave idempotente do fluxo de carga geral")
    private UUID commandId;

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Código do agendamento associado ao movimento")
    private String agendamentoCodigo;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Número do Bill of Lading")
    private String blNumero;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Número da delivery order")
    private String deliveryOrder;

    @NotNull
    @Schema(description = "Identificador do cargo lot")
    private UUID loteId;

    @NotNull
    @Schema(description = "Tipo do movimento parcial")
    private TipoMovimentoCargaGeral tipoMovimento;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantidade;

    @NotNull
    @DecimalMin("0.000")
    private BigDecimal volumeM3;

    @NotNull
    @DecimalMin("0.000")
    private BigDecimal pesoKg;

    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    public String getAgendamentoCodigo() {
        return agendamentoCodigo;
    }

    public void setAgendamentoCodigo(String agendamentoCodigo) {
        this.agendamentoCodigo = agendamentoCodigo;
    }

    public String getBlNumero() {
        return blNumero;
    }

    public void setBlNumero(String blNumero) {
        this.blNumero = blNumero;
    }

    public String getDeliveryOrder() {
        return deliveryOrder;
    }

    public void setDeliveryOrder(String deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public void setLoteId(UUID loteId) {
        this.loteId = loteId;
    }

    public TipoMovimentoCargaGeral getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimentoCargaGeral tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getVolumeM3() {
        return volumeM3;
    }

    public void setVolumeM3(BigDecimal volumeM3) {
        this.volumeM3 = volumeM3;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public enum TipoMovimentoCargaGeral {
        RETIRADA,
        ENTREGA
    }
}
