package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.ResultadoAvaria;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusAvariaOperacional;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusInventarioFisico;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusPlanoOperacional;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusReservaGate;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.TipoUnidadeIntermodal;
import br.com.cloudport.servicocargageral.dominio.TipoServicoOrdemCarga;
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

public final class OperacaoIntermodalDTOs {

    private OperacaoIntermodalDTOs() {
    }

    public record CriarPlanoRequest(
            @NotBlank @Size(max = 80) String numero,
            @NotNull TipoServicoOrdemCarga tipo,
            @NotNull @Min(1) @Max(999) Integer prioridade,
            @NotNull OffsetDateTime janelaInicio,
            @NotNull OffsetDateTime janelaFim,
            @NotBlank @Size(max = 120) String local,
            TipoUnidadeIntermodal origemTipo,
            @Size(max = 120) String origemId,
            TipoUnidadeIntermodal destinoTipo,
            @Size(max = 120) String destinoId,
            @Size(max = 80) String visitaNavioId,
            @Size(max = 80) String visitaFerroviariaId,
            @Size(max = 80) String equipeId,
            @Size(max = 80) String equipamentoId,
            @Size(max = 80) String lacreOrigem,
            @Size(max = 80) String lacreDestino,
            @Size(max = 2000) String restricoes,
            @Size(max = 2000) String instrucaoTrabalho,
            @NotBlank @Size(max = 120) String usuario,
            @Valid @NotEmpty List<CriarItemPlanoRequest> itens) {
    }

    public record CriarItemPlanoRequest(
            @NotNull UUID loteId,
            @NotNull @Min(1) Integer sequencia,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadePlanejada,
            @NotNull @DecimalMin("0.000") BigDecimal volumePlanejadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoPlanejadoKg,
            @Size(max = 120) String posicaoPlanejada,
            @Size(max = 120) String areaPorao,
            @Size(max = 80) String vagaoId,
            @Size(max = 80) String posicaoVagao,
            @DecimalMin("0.000") BigDecimal capacidadeReservadaKg) {
    }

    public record NovaVersaoPlanoRequest(
            @NotBlank @Size(max = 80) String novoNumero,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ComandoPlanoRequest(
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 1000) String observacao) {
    }

    public record AtribuirRecursosRequest(
            @Size(max = 80) String equipeId,
            @Size(max = 80) String equipamentoId,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ExecutarItemPlanoRequest(
            @NotBlank @Size(max = 120) String commandId,
            @NotNull UUID itemId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @Size(max = 120) String posicaoOrigemReal,
            @Size(max = 120) String posicaoDestinoReal,
            @Size(max = 1000) String divergencia,
            @Size(max = 80) String codigoAvaria,
            @Size(max = 1000) String descricaoAvaria,
            @DecimalMin("0.000") BigDecimal quantidadeAvariada,
            @DecimalMin("0.000") BigDecimal volumeAvariadoM3,
            @DecimalMin("0.000") BigDecimal pesoAvariadoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ConcluirPlanoRequest(
            boolean aceitarDivergencia,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 1000) String observacao) {
    }

    public record CancelarPlanoRequest(
            @NotBlank @Size(max = 1000) String motivo,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record PlanoResposta(
            UUID id,
            String numero,
            TipoServicoOrdemCarga tipo,
            StatusPlanoOperacional status,
            Integer prioridade,
            Integer versaoPlano,
            UUID planoOrigemId,
            OffsetDateTime janelaInicio,
            OffsetDateTime janelaFim,
            String local,
            TipoUnidadeIntermodal origemTipo,
            String origemId,
            TipoUnidadeIntermodal destinoTipo,
            String destinoId,
            String visitaNavioId,
            String visitaFerroviariaId,
            String equipeId,
            String equipamentoId,
            String lacreOrigem,
            String lacreDestino,
            String restricoes,
            String instrucaoTrabalho,
            String motivoCancelamento,
            String historicoOperacional,
            OffsetDateTime criadoEm,
            OffsetDateTime liberadoEm,
            OffsetDateTime iniciadoEm,
            OffsetDateTime concluidoEm,
            List<ItemPlanoResposta> itens) {
    }

    public record ItemPlanoResposta(
            UUID id,
            UUID loteId,
            String loteCodigo,
            Integer sequencia,
            BigDecimal quantidadePlanejada,
            BigDecimal quantidadeRealizada,
            BigDecimal volumePlanejadoM3,
            BigDecimal volumeRealizadoM3,
            BigDecimal pesoPlanejadoKg,
            BigDecimal pesoRealizadoKg,
            String posicaoPlanejada,
            String posicaoOrigemReal,
            String posicaoDestinoReal,
            String areaPorao,
            String vagaoId,
            String posicaoVagao,
            BigDecimal capacidadeReservadaKg,
            String divergencia,
            String codigoAvaria,
            String descricaoAvaria) {
    }

    public record CriarReservaGateRequest(
            @NotBlank @Size(max = 120) String transacaoId,
            boolean retirada,
            @NotNull UUID loteId,
            @NotBlank @Size(max = 80) String blNumero,
            @Size(max = 120) String deliveryOrder,
            @Size(max = 120) String appointmentId,
            @Size(max = 120) String truckVisitId,
            @NotBlank @Size(max = 80) String veiculoId,
            OffsetDateTime janelaInicio,
            OffsetDateTime janelaFim,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ConfirmarReservaGateRequest(
            @NotBlank @Size(max = 120) String confirmacaoId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record LiberarReservaGateRequest(
            @NotBlank @Size(max = 1000) String motivo) {
    }

    public record ReservaGateResposta(
            UUID id,
            String transacaoId,
            boolean retirada,
            StatusReservaGate status,
            UUID loteId,
            String loteCodigo,
            String blNumero,
            String deliveryOrder,
            String appointmentId,
            String truckVisitId,
            String veiculoId,
            BigDecimal quantidadeReservada,
            BigDecimal quantidadeConfirmada,
            BigDecimal quantidadePendente,
            BigDecimal volumeReservadoM3,
            BigDecimal volumeConfirmadoM3,
            BigDecimal volumePendenteM3,
            BigDecimal pesoReservadoKg,
            BigDecimal pesoConfirmadoKg,
            BigDecimal pesoPendenteKg,
            OffsetDateTime criadoEm,
            List<ConfirmacaoGateResposta> confirmacoes) {
    }

    public record ConfirmacaoGateResposta(
            UUID id,
            String confirmacaoId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String usuario,
            String correlationId,
            OffsetDateTime confirmadoEm) {
    }

    public record RegistrarAvariaRequest(
            @NotNull UUID loteId,
            @NotBlank @Size(max = 80) String codigo,
            @NotBlank @Size(max = 1000) String descricao,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadeAfetada,
            @NotNull @DecimalMin("0.000") BigDecimal volumeAfetadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoAfetadoKg,
            @NotBlank @Size(max = 120) String responsavel,
            @Size(max = 8000) String evidenciasJson) {
    }

    public record InspecionarAvariaRequest(
            @NotBlank @Size(max = 4000) String relatorio,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record EncerrarAvariaRequest(
            @NotNull ResultadoAvaria resultado,
            @NotBlank @Size(max = 2000) String observacao,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record AvariaResposta(
            UUID id,
            UUID loteId,
            String loteCodigo,
            String codigo,
            String descricao,
            BigDecimal quantidadeAfetada,
            BigDecimal volumeAfetadoM3,
            BigDecimal pesoAfetadoKg,
            String responsavel,
            String evidenciasJson,
            StatusAvariaOperacional status,
            String relatorioInspecao,
            ResultadoAvaria resultadoTratamento,
            String historicoOperacional,
            OffsetDateTime criadoEm,
            OffsetDateTime encerradoEm) {
    }

    public record AbrirInventarioRequest(
            @NotBlank @Size(max = 80) String codigo,
            @NotBlank @Size(max = 80) String armazemId,
            @Size(max = 120) String posicaoReferencia,
            @NotBlank @Size(max = 1000) String motivo,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record RegistrarContagemRequest(
            @NotBlank @Size(max = 160) String codigoIdentificacao,
            @NotBlank @Size(max = 120) String posicao,
            @NotNull @Min(1) Integer numeroContagem,
            @NotNull @DecimalMin("0.000") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record ConciliarInventarioRequest(
            @NotBlank @Size(max = 120) String aprovador,
            @NotBlank @Size(max = 2000) String justificativa) {
    }

    public record InventarioResposta(
            UUID id,
            String codigo,
            String armazemId,
            String posicaoReferencia,
            String motivo,
            StatusInventarioFisico status,
            String abertoPor,
            String aprovadoPor,
            String justificativaAjuste,
            String historicoOperacional,
            OffsetDateTime criadoEm,
            OffsetDateTime concluidoEm,
            List<ContagemResposta> contagens) {
    }

    public record ContagemResposta(
            UUID id,
            UUID loteId,
            String loteCodigo,
            String codigoIdentificacao,
            String posicao,
            Integer numeroContagem,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String usuario,
            String correlationId,
            OffsetDateTime contadoEm) {
    }
}
