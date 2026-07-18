package br.com.cloudport.servicogate.app.billing.dto;

import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class BillingCapDtos {

    private BillingCapDtos() {
    }

    public record TarifaRequest(
            @NotBlank @Size(max = 50) @Schema(example = "GATE_ENTRADA") String codigo,
            @NotBlank @Size(max = 160) String descricao,
            @NotNull TipoOperacao tipoOperacao,
            @NotNull @DecimalMin(value = "0.00") BigDecimal valor,
            @NotNull LocalDate inicioVigencia,
            LocalDate fimVigencia,
            Boolean ativa) {
    }

    public record TarifaDTO(
            Long id,
            String codigo,
            String descricao,
            TipoOperacao tipoOperacao,
            BigDecimal valor,
            LocalDate inicioVigencia,
            LocalDate fimVigencia,
            boolean ativa) {
    }

    public record CobrancaDTO(
            Long id,
            String referencia,
            Long transportadoraId,
            String transportadora,
            Long agendamentoId,
            String agendamento,
            String descricao,
            BigDecimal valor,
            String status,
            LocalDateTime ocorridoEm,
            LocalDateTime faturadoEm) {
    }

    public record FaturaGeracaoRequest(
            @NotNull Long transportadoraId,
            @NotNull LocalDate vencimento,
            List<Long> cobrancaIds) {
    }

    public record PagamentoRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
            @NotBlank @Size(max = 40) String forma,
            @Size(max = 100) String referencia,
            LocalDateTime pagoEm) {
    }

    public record FaturaItemDTO(
            Long id,
            Long cobrancaId,
            String descricao,
            BigDecimal valor) {
    }

    public record PagamentoDTO(
            Long id,
            BigDecimal valor,
            String forma,
            String referencia,
            LocalDateTime pagoEm) {
    }

    public record FaturaDTO(
            Long id,
            String numero,
            Long transportadoraId,
            String transportadora,
            LocalDateTime emitidaEm,
            LocalDate vencimento,
            String status,
            BigDecimal subtotal,
            BigDecimal total,
            BigDecimal valorPago,
            BigDecimal saldo,
            LocalDateTime pagoEm,
            List<FaturaItemDTO> itens,
            List<PagamentoDTO> pagamentos) {
    }

    public record CapAgendamentoDTO(
            Long id,
            String codigo,
            TipoOperacao tipoOperacao,
            String status,
            LocalDateTime horarioPrevistoChegada,
            LocalDateTime horarioRealChegada,
            LocalDateTime horarioRealSaida) {
    }

    public record CapResumoDTO(
            Long transportadoraId,
            String transportadora,
            long totalAgendamentos,
            long agendamentosPendentes,
            long agendamentosConcluidos,
            long cobrancasPendentes,
            BigDecimal valorCobrancasPendentes,
            long faturasAbertas,
            BigDecimal valorFaturasAbertas,
            List<CapAgendamentoDTO> agendamentosRecentes,
            List<FaturaDTO> faturasRecentes) {
    }
}
