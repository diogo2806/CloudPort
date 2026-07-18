package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.EstagioGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.ModalTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAlocacaoCargoLot;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAvariaCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusDivergenciaInventarioCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusInventarioFisicoCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusPlanoTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusReservaGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusTransload;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoMovimentoGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoOperacaoTransporteCargo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class OperacoesIntermodaisDTOs {

    private OperacoesIntermodaisDTOs() {
    }

    public record ExecutarTransloadRequest(
            @NotNull UUID commandId,
            @NotBlank @Size(max = 80) String unidadeOrigem,
            @NotBlank @Size(max = 80) String unidadeDestino,
            @Size(max = 80) String lacreOrigem,
            @Size(max = 80) String lacreDestino,
            @Size(max = 1000) String divergencia,
            @Size(max = 80) String codigoAvaria,
            @Size(max = 1000) String descricaoAvaria,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @Valid @NotEmpty List<ItemTransloadRequest> itens) {
    }

    public record ItemTransloadRequest(
            @NotNull UUID loteOrigemId,
            @NotNull UUID loteDestinoId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg) {
    }

    public record TransloadResposta(
            UUID id,
            UUID commandId,
            String unidadeOrigem,
            String unidadeDestino,
            String lacreOrigem,
            String lacreDestino,
            String divergencia,
            String codigoAvaria,
            StatusTransload status,
            String usuario,
            OffsetDateTime executadoEm,
            List<ItemTransloadResposta> itens) {
    }

    public record ItemTransloadResposta(
            UUID loteOrigemId,
            UUID loteDestinoId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg) {
    }

    public record ReservarGateCargaRequest(
            @NotNull UUID commandId,
            @NotBlank @Size(max = 80) String agendamentoCodigo,
            @NotBlank @Size(max = 100) String blNumero,
            @NotBlank @Size(max = 100) String deliveryOrder,
            @NotNull UUID loteId,
            @NotNull TipoMovimentoGateCarga tipoMovimento,
            @NotNull EstagioGateCarga estagioConfirmacao,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ConfirmarGateCargaRequest(
            @NotNull UUID commandId,
            @NotNull EstagioGateCarga estagio,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record CompensarGateCargaRequest(
            @NotNull UUID commandId,
            @NotBlank @Size(max = 1000) String motivo,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ReservaGateCargaResposta(
            UUID id,
            String agendamentoCodigo,
            String blNumero,
            String deliveryOrder,
            UUID loteId,
            TipoMovimentoGateCarga tipoMovimento,
            EstagioGateCarga estagioConfirmacao,
            StatusReservaGateCarga status,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            OffsetDateTime reservadoEm,
            OffsetDateTime confirmadoEm,
            OffsetDateTime compensadoEm) {
    }

    public record CriarAlocacaoRequest(
            @NotNull UUID commandId,
            @NotNull UUID loteId,
            @Size(max = 120) String origem,
            @NotBlank @Size(max = 120) String destino,
            @NotBlank @Size(max = 120) String recurso,
            @Min(1) @Max(999) int prioridade,
            @Size(max = 1000) String restricoes,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ComandoMotivadoRequest(
            @NotBlank @Size(max = 120) String usuario,
            @NotBlank @Size(max = 1000) String motivo) {
    }

    public record AlocacaoResposta(
            UUID id,
            UUID loteId,
            UUID reservaCapacidadeId,
            String origem,
            String destino,
            String recurso,
            int prioridade,
            String restricoes,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            StatusAlocacaoCargoLot status,
            OffsetDateTime criadoEm,
            OffsetDateTime confirmadoEm) {
    }

    public record PlanejarTransporteRequest(
            @NotNull UUID commandId,
            @NotNull ModalTransporteCargo modal,
            @NotNull TipoOperacaoTransporteCargo tipoOperacao,
            @NotBlank @Size(max = 80) String visitaId,
            @NotBlank @Size(max = 100) String blNumero,
            @NotNull UUID loteId,
            @NotBlank @Size(max = 120) String compartimento,
            @Size(max = 120) String posicao,
            @Min(1) int sequencia,
            @NotBlank @Size(max = 120) String equipamento,
            @Size(max = 120) String custodia,
            @Size(max = 1000) String restricoes,
            @DecimalMin("0.000") BigDecimal capacidadePesoKg,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadePlanejada,
            @NotNull @DecimalMin("0.000") BigDecimal volumePlanejadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoPlanejadoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ExecutarTransporteRequest(
            @NotNull UUID commandId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record PlanoTransporteResposta(
            UUID id,
            ModalTransporteCargo modal,
            TipoOperacaoTransporteCargo tipoOperacao,
            StatusPlanoTransporteCargo status,
            String visitaId,
            String blNumero,
            UUID loteId,
            String compartimento,
            String posicao,
            int sequencia,
            String equipamento,
            String custodia,
            String restricoes,
            BigDecimal quantidadePlanejada,
            BigDecimal volumePlanejadoM3,
            BigDecimal pesoPlanejadoKg,
            BigDecimal quantidadeRealizada,
            BigDecimal volumeRealizadoM3,
            BigDecimal pesoRealizadoKg,
            OffsetDateTime planejadoEm,
            OffsetDateTime executadoEm) {
    }

    public record AbrirAvariaRequest(
            @NotNull UUID commandId,
            @NotNull UUID loteId,
            @NotBlank @Size(max = 80) String codigo,
            @NotBlank @Size(max = 1000) String descricao,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadeAfetada,
            @NotNull @DecimalMin("0.000") BigDecimal volumeAfetadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoAfetadoKg,
            @NotBlank @Size(max = 120) String responsavel) {
    }

    public record AdicionarEvidenciaAvariaRequest(
            @NotBlank @Size(max = 40) String tipo,
            @NotBlank @Size(max = 1000) String uri,
            @Size(max = 128) String checksum,
            @NotBlank @Size(max = 120) String responsavel) {
    }

    public record TransicionarAvariaRequest(
            @NotBlank @Size(max = 30) String acao,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 1000) String observacao) {
    }

    public record AvariaResposta(
            UUID id,
            UUID loteId,
            String codigo,
            String descricao,
            BigDecimal quantidadeAfetada,
            BigDecimal volumeAfetadoM3,
            BigDecimal pesoAfetadoKg,
            StatusAvariaCarga status,
            String responsavel,
            String inspecionadoPor,
            String reparadoPor,
            String observacoes,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm,
            List<EvidenciaAvariaResposta> evidencias) {
    }

    public record EvidenciaAvariaResposta(
            String tipo,
            String uri,
            String checksum,
            String responsavel,
            OffsetDateTime registradoEm) {
    }

    public record AbrirInventarioFisicoRequest(
            @NotNull UUID commandId,
            @NotBlank @Size(max = 120) String posicao,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record RegistrarContagemRequest(
            @NotNull UUID commandId,
            @NotNull UUID loteId,
            @NotBlank @Size(max = 160) String identificacao,
            @NotNull @DecimalMin("0.000") BigDecimal quantidadeContada,
            @NotNull @DecimalMin("0.000") BigDecimal volumeContadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoContadoKg,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 1000) String observacao) {
    }

    public record ResolverDivergenciaRequest(
            @NotNull UUID commandIdContagem,
            boolean ajustarSaldo,
            @NotBlank @Size(max = 120) String usuario,
            @NotBlank @Size(max = 1000) String motivo) {
    }

    public record InventarioFisicoResposta(
            UUID id,
            String posicao,
            StatusInventarioFisicoCargo status,
            String abertoPor,
            OffsetDateTime abertoEm,
            String concluidoPor,
            OffsetDateTime concluidoEm,
            String motivo,
            List<ContagemResposta> contagens) {
    }

    public record ContagemResposta(
            UUID commandId,
            UUID loteId,
            String identificacao,
            BigDecimal quantidadeLogica,
            BigDecimal volumeLogicoM3,
            BigDecimal pesoLogicoKg,
            BigDecimal quantidadeContada,
            BigDecimal volumeContadoM3,
            BigDecimal pesoContadoKg,
            StatusDivergenciaInventarioCargo statusDivergencia,
            String usuario,
            String observacao,
            OffsetDateTime contadoEm,
            String resolvidoPor,
            String motivoResolucao) {
    }
}
