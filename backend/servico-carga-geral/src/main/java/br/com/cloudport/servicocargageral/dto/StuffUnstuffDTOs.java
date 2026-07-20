package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.MetodoPesagemVgm;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLacreStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusPesagemVgm;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoLacreStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao.StatusPlano;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class StuffUnstuffDTOs {

    private StuffUnstuffDTOs() {
    }

    public record CriarOperacaoRequest(
            @NotNull TipoOperacaoStuffUnstuff tipo,
            @NotBlank @Size(max = 80) String conteinerId,
            @Size(max = 80) String armazemId,
            @Size(max = 120) String posicaoOperacao,
            @Size(max = 120) String equipeRecurso,
            @Size(max = 80) String lacreInicial,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @Valid @NotEmpty List<CriarItemOperacaoRequest> itens) {
    }

    public record CriarItemOperacaoRequest(
            @NotNull UUID loteId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadePlanejada,
            @NotNull @DecimalMin("0.000") BigDecimal volumePlanejadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoPlanejadoKg) {
    }

    public record CriarVersaoPlanoRequest(
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @NotBlank @Size(max = 1000) String motivo,
            @Valid @NotEmpty List<CriarItemOperacaoRequest> itens) {
    }

    public record LiberarPlanoRequest(
            @Min(1) int versao,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @NotBlank @Size(max = 1000) String motivo) {
    }

    public record RegistrarExecucaoRequest(
            @NotNull UUID commandId,
            @NotNull UUID itemId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @Size(max = 80) String codigoAvaria,
            @Size(max = 1000) String descricaoAvaria,
            @Size(max = 1000) String divergencia,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record RegistrarLacreRequest(
            @NotNull UUID commandId,
            @NotNull TipoEventoLacreStuffUnstuff tipoEvento,
            @NotBlank @Size(max = 80) String numeroLacre,
            @Size(max = 80) String numeroLacreSubstituido,
            @NotBlank @Size(max = 120) String operador,
            @Size(max = 120) String correlationId,
            @Size(max = 1000) String motivo,
            boolean divergencia,
            boolean overrideAutorizado) {
    }

    public record ConfirmarPesagemStuffingRequest(
            @NotNull MetodoPesagemVgm metodoPesagem,
            @NotNull @DecimalMin("0.001") BigDecimal taraKg,
            @NotNull @DecimalMin("0.001") BigDecimal pesoBrutoKg,
            @NotNull @DecimalMin("0.001") BigDecimal vgmKg,
            @NotNull @DecimalMin("0.001") BigDecimal capacidadeMaximaKg,
            @NotBlank @Size(max = 120) String equipamentoPesagem,
            @NotBlank @Size(max = 120) String responsavelPesagem,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @Size(max = 1000) String observacao) {
    }

    public record ConcluirOperacaoRequest(
            @Size(max = 80) String lacreFinal,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @Size(max = 1000) String observacao) {
    }

    public record CancelarOperacaoRequest(
            @NotBlank @Size(max = 1000) String motivo,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record OperacaoResposta(
            UUID id,
            TipoOperacaoStuffUnstuff tipo,
            StatusOperacaoStuffUnstuff status,
            String conteinerId,
            String armazemId,
            String posicaoOperacao,
            String equipeRecurso,
            String lacreInicial,
            String lacreFinal,
            String motivoCancelamento,
            MetodoPesagemVgm metodoPesagem,
            StatusPesagemVgm statusPesagemVgm,
            BigDecimal taraKg,
            BigDecimal pesoBrutoKg,
            BigDecimal vgmKg,
            BigDecimal capacidadeMaximaKg,
            String equipamentoPesagem,
            String responsavelPesagem,
            OffsetDateTime pesagemConfirmadaEm,
            String motivoBloqueioPeso,
            OffsetDateTime criadoEm,
            OffsetDateTime iniciadoEm,
            OffsetDateTime concluidoEm,
            OffsetDateTime canceladoEm,
            List<ItemOperacaoResposta> itens,
            List<EventoOperacaoResposta> historico) {
    }

    public record PlanoVersaoResposta(
            UUID id,
            int versao,
            StatusPlano status,
            String criadoPor,
            OffsetDateTime criadoEm,
            String liberadoPor,
            OffsetDateTime liberadoEm,
            String motivo,
            List<ItemPlanoResposta> itens) {
    }

    public record ItemPlanoResposta(
            UUID loteId,
            String loteCodigo,
            BigDecimal quantidadePlanejada,
            BigDecimal volumePlanejadoM3,
            BigDecimal pesoPlanejadoKg) {
    }

    public record ItemOperacaoResposta(
            UUID id,
            UUID loteId,
            String loteCodigo,
            BigDecimal quantidadePlanejada,
            BigDecimal quantidadeRealizada,
            BigDecimal volumePlanejadoM3,
            BigDecimal volumeRealizadoM3,
            BigDecimal pesoPlanejadoKg,
            BigDecimal pesoRealizadoKg,
            String codigoAvaria,
            String descricaoAvaria,
            String divergencia) {
    }

    public record EventoOperacaoResposta(
            UUID id,
            TipoEventoStuffUnstuff tipo,
            String usuario,
            String correlationId,
            String descricao,
            OffsetDateTime ocorridoEm) {
    }

    public record LacreOperacaoResposta(
            UUID id,
            UUID commandId,
            String numeroLacre,
            String numeroLacreSubstituido,
            TipoEventoLacreStuffUnstuff tipoEvento,
            StatusLacreStuffUnstuff status,
            String operador,
            String correlationId,
            String motivo,
            boolean divergenciaAberta,
            boolean overrideAutorizado,
            OffsetDateTime ocorridoEm) {
    }
}
