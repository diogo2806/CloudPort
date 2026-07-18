package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
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

public final class OperacaoStuffUnstuffDTOs {

    private OperacaoStuffUnstuffDTOs() {
    }

    public record CriarOperacaoRequest(
            @NotBlank @Size(max = 80) String numero,
            @NotNull TipoOperacaoStuffUnstuff tipo,
            @NotBlank @Size(max = 80) String containerId,
            @Size(max = 80) String armazemId,
            @Size(max = 120) String posicaoOperacao,
            @Size(max = 160) String equipeRecurso,
            @Size(max = 80) String lacreInicial,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 1000) String observacao,
            @NotEmpty List<@Valid CriarItemOperacaoRequest> itens) {
    }

    public record CriarItemOperacaoRequest(
            @NotNull UUID loteId,
            @NotNull @DecimalMin("0.000") BigDecimal quantidadePlanejada,
            @NotNull @DecimalMin("0.000") BigDecimal volumePlanejadoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoPlanejadoKg) {
    }

    public record RegistrarExecucaoRequest(
            @NotNull UUID itemId,
            @NotNull @DecimalMin("0.000") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @Size(max = 1000) String divergencia,
            @Size(max = 80) String codigoAvaria,
            @Size(max = 1000) String descricaoAvaria) {
    }

    public record ConcluirOperacaoRequest(@Size(max = 80) String lacreFinal) {
    }

    public record CancelarOperacaoRequest(@NotBlank @Size(max = 1000) String motivo) {
    }

    public record ItemOperacaoResposta(
            UUID id,
            UUID loteId,
            String loteCodigo,
            BigDecimal quantidadePlanejada,
            BigDecimal volumePlanejadoM3,
            BigDecimal pesoPlanejadoKg,
            BigDecimal quantidadeRealizada,
            BigDecimal volumeRealizadoM3,
            BigDecimal pesoRealizadoKg,
            String divergencia,
            String codigoAvaria,
            String descricaoAvaria) {
    }

    public record OperacaoResposta(
            UUID id,
            String numero,
            TipoOperacaoStuffUnstuff tipo,
            StatusOperacaoStuffUnstuff status,
            String containerId,
            String armazemId,
            String posicaoOperacao,
            String equipeRecurso,
            String lacreInicial,
            String lacreFinal,
            String usuario,
            String observacao,
            String motivoCancelamento,
            OffsetDateTime iniciadaEm,
            OffsetDateTime concluidaEm,
            OffsetDateTime canceladaEm,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm,
            List<ItemOperacaoResposta> itens) {
    }
}
