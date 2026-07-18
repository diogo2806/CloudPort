package br.com.cloudport.servicogate.app.operacional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class GateOperacionalDtos {

    private GateOperacionalDtos() {
    }

    public record FacilityRequest(
            @NotBlank @Size(max = 40) @Schema(example = "TERMINAL-PRINCIPAL") String codigo,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 60) @Schema(example = "America/Sao_Paulo") String fusoHorario,
            Boolean ativo) {
    }

    public record GateRequest(
            @NotNull Long facilityId,
            @NotBlank @Size(max = 40) @Schema(example = "GATE-01") String codigo,
            @NotBlank @Size(max = 120) String nome,
            Boolean ativo) {
    }

    public record LaneRequest(
            @NotNull Long gateId,
            @NotBlank @Size(max = 40) @Schema(example = "IN-01") String codigo,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Schema(allowableValues = {"ENTRADA", "SAIDA", "BIDIRECIONAL"}) String direcao,
            @NotBlank @Schema(allowableValues = {"ABERTA", "FECHADA", "MANUTENCAO"}) String status,
            @NotNull @Min(1) @Max(500) Integer capacidadeFila,
            Boolean ocrHabilitado,
            Boolean balancaHabilitada,
            Boolean inspecaoHabilitada) {
    }

    public record StageRequest(
            @NotNull Long gateId,
            @NotBlank @Size(max = 40) @Schema(example = "PRE_CHECK") String codigo,
            @NotBlank @Size(max = 120) String nome,
            @NotNull @Min(1) Integer ordem,
            Boolean ativo,
            Boolean permiteTrouble,
            Boolean finalizaVisita,
            @Size(max = 40) String codigoStageAnterior) {
    }

    public record BusinessTaskRequest(
            @NotBlank @Size(max = 60) String codigo,
            @NotBlank @Size(max = 160) String nome,
            @NotBlank @Schema(allowableValues = {"VALIDACAO", "CAPTURA", "INTEGRACAO", "IMPRESSAO", "DECISAO"}) String tipo,
            @NotNull @Min(1) Integer ordem,
            Boolean obrigatoria,
            Boolean ativa,
            Map<String, Object> configuracao) {
    }

    public record AccessRuleRequest(
            @NotNull Long gateId,
            @NotBlank @Schema(allowableValues = {"MOTORISTA", "TRANSPORTADORA", "VEICULO"}) String escopo,
            @NotNull Long referenciaId,
            @NotBlank @Schema(allowableValues = {"BLOQUEIO", "PERMISSAO"}) String tipo,
            @NotBlank @Size(max = 500) String motivo,
            LocalDateTime inicioVigencia,
            LocalDateTime fimVigencia,
            Boolean ativo) {
    }

    public record BookingRequest(
            @NotBlank @Size(max = 60) String codigo,
            Long transportadoraId,
            @Size(max = 120) String armador,
            @Size(max = 80) String viagem,
            @NotNull @Min(1) Integer quantidadeTotal,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim,
            @Size(max = 500) String observacoes) {
    }

    public record BillOfLadingRequest(
            @NotBlank @Size(max = 80) String numero,
            @Size(max = 120) String armador,
            @Size(max = 80) String viagem,
            @Size(max = 160) String consignatario,
            @NotNull @Min(1) Integer quantidadeTotal,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim,
            @Size(max = 500) String observacoes) {
    }

    public record OrderRequest(
            @NotBlank @Schema(allowableValues = {"EDO", "ERO", "IDO"}) String tipo,
            @NotBlank @Size(max = 60) String codigo,
            Long bookingId,
            Long billOfLadingId,
            Long transportadoraId,
            @Size(max = 30) String unidadeReferencia,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim) {

        public OrderRequest(String tipo, String codigo, Long bookingId, Long transportadoraId,
                            String unidadeReferencia, LocalDateTime validadeInicio, LocalDateTime validadeFim) {
            this(tipo, codigo, bookingId, null, transportadoraId, unidadeReferencia, validadeInicio, validadeFim);
        }
    }

    public record PreadviceRequest(
            @NotBlank @Schema(allowableValues = {"EXPORTACAO", "VAZIO"}) String tipo,
            @NotBlank @Size(max = 60) String codigo,
            Long bookingId,
            Long orderId,
            @Size(max = 30) String unidadeReferencia,
            @Size(max = 10) String isoType,
            BigDecimal pesoBrutoKg) {
    }

    public record TransactionRequest(
            @NotBlank @Size(max = 40) String tipoOperacao,
            @Size(max = 30) String unidadeReferencia,
            Long bookingId,
            Long billOfLadingId,
            Long orderId,
            Long preadviceId) {

        public TransactionRequest(String tipoOperacao, String unidadeReferencia, Long bookingId,
                                  Long orderId, Long preadviceId) {
            this(tipoOperacao, unidadeReferencia, bookingId, null, orderId, preadviceId);
        }
    }

    public record TruckVisitRequest(
            Long agendamentoId,
            @NotNull Long facilityId,
            @NotNull Long gateId,
            Long laneId,
            @NotNull Long transportadoraId,
            @NotNull Long motoristaId,
            @NotNull Long veiculoId,
            @NotEmpty @Size(max = 20) List<@Valid TransactionRequest> transacoes,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record AdvanceVisitRequest(
            @Size(max = 40) String destinoStageCodigo,
            Long laneId,
            Set<@Size(max = 60) String> tarefasConcluidas,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record TroubleRequest(
            @NotBlank @Size(max = 60) String codigo,
            @NotBlank @Size(max = 500) String descricao,
            @NotBlank @Schema(allowableValues = {"BAIXA", "MEDIA", "ALTA", "CRITICA"}) String severidade,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record TroubleResolutionRequest(
            @NotBlank @Size(max = 500) String resolucao,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record InspectionRequest(
            @NotBlank @Size(max = 40) String tipo,
            @NotBlank @Schema(allowableValues = {"APROVADO", "REPROVADO", "COM_RESSALVA"}) String resultado,
            @Size(max = 500) String observacoes,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record AttachmentRequest(
            Long truckVisitId,
            Long transactionId,
            @NotBlank @Schema(allowableValues = {"FOTOGRAFIA", "DOCUMENTO", "AVARIA", "OCR", "INSPECAO"}) String categoria,
            @NotBlank @Size(max = 255) String nomeArquivo,
            @Size(max = 120) String contentType,
            @NotBlank @Size(max = 500) String urlDocumento,
            Map<String, Object> metadados,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record DocumentRequest(
            Long transactionId,
            @NotBlank @Schema(allowableValues = {"TICKET", "EIR", "COMPROVANTE_TRANSFERENCIA"}) String tipo,
            Map<String, Object> conteudo,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record TransferRequest(
            @NotNull Long facilityDestinoId,
            @Size(max = 500) String observacoes,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId) {
    }

    public record FacilityDTO(Long id, String codigo, String nome, String fusoHorario, boolean ativo) {
    }

    public record GateDTO(Long id, Long facilityId, String codigo, String nome, boolean ativo) {
    }

    public record LaneDTO(
            Long id,
            Long gateId,
            String codigo,
            String nome,
            String direcao,
            String status,
            int capacidadeFila,
            int filaAtual,
            boolean ocrHabilitado,
            boolean balancaHabilitada,
            boolean inspecaoHabilitada) {
    }

    public record BusinessTaskDTO(
            Long id,
            Long stageId,
            String codigo,
            String nome,
            String tipo,
            int ordem,
            boolean obrigatoria,
            boolean ativa,
            Map<String, Object> configuracao) {
    }

    public record StageDTO(
            Long id,
            Long gateId,
            String codigo,
            String nome,
            int ordem,
            boolean ativo,
            boolean permiteTrouble,
            boolean finalizaVisita,
            List<BusinessTaskDTO> tarefas) {
    }

    public record AccessRuleDTO(
            Long id,
            Long gateId,
            String escopo,
            Long referenciaId,
            String tipo,
            String motivo,
            LocalDateTime inicioVigencia,
            LocalDateTime fimVigencia,
            boolean ativo) {
    }

    public record BookingDTO(
            Long id,
            String codigo,
            Long transportadoraId,
            String armador,
            String viagem,
            int quantidadeTotal,
            int quantidadeUtilizada,
            String status,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim,
            String observacoes) {
    }

    public record BillOfLadingDTO(
            Long id,
            String numero,
            String armador,
            String viagem,
            String consignatario,
            int quantidadeTotal,
            int quantidadeLiberada,
            String status,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim,
            String observacoes) {
    }

    public record OrderDTO(
            Long id,
            String tipo,
            String codigo,
            Long bookingId,
            Long billOfLadingId,
            Long transportadoraId,
            String unidadeReferencia,
            String status,
            LocalDateTime validadeInicio,
            LocalDateTime validadeFim) {

        public OrderDTO(Long id, String tipo, String codigo, Long bookingId, Long transportadoraId,
                        String unidadeReferencia, String status, LocalDateTime validadeInicio,
                        LocalDateTime validadeFim) {
            this(id, tipo, codigo, bookingId, null, transportadoraId, unidadeReferencia, status,
                    validadeInicio, validadeFim);
        }
    }

    public record PreadviceDTO(
            Long id,
            String tipo,
            String codigo,
            Long bookingId,
            Long orderId,
            String unidadeReferencia,
            String isoType,
            BigDecimal pesoBrutoKg,
            String status) {
    }

    public record TransactionDTO(
            Long id,
            Long truckVisitId,
            int sequencia,
            String tipoOperacao,
            String status,
            String unidadeReferencia,
            Long bookingId,
            Long billOfLadingId,
            Long orderId,
            Long preadviceId,
            Long stageAtualId,
            boolean troubleAtivo) {

        public TransactionDTO(Long id, Long truckVisitId, int sequencia, String tipoOperacao,
                              String status, String unidadeReferencia, Long bookingId, Long orderId,
                              Long preadviceId, Long stageAtualId, boolean troubleAtivo) {
            this(id, truckVisitId, sequencia, tipoOperacao, status, unidadeReferencia, bookingId,
                    null, orderId, preadviceId, stageAtualId, troubleAtivo);
        }
    }

    public record TruckVisitDTO(
            Long id,
            String codigo,
            Long agendamentoId,
            Long facilityId,
            Long gateId,
            Long laneId,
            String laneCodigo,
            Long transportadoraId,
            String transportadora,
            Long motoristaId,
            String motorista,
            Long veiculoId,
            String placa,
            Long stageAtualId,
            String stageAtualCodigo,
            String stageAtualNome,
            String status,
            LocalDateTime checkinEm,
            LocalDateTime checkoutEm,
            LocalDateTime iniciadoEm,
            LocalDateTime finalizadoEm,
            List<TransactionDTO> transacoes) {
    }

    public record TroubleDTO(
            Long id,
            Long transactionId,
            Long truckVisitId,
            String truckVisitCodigo,
            String codigo,
            String descricao,
            String severidade,
            String status,
            String abertoPor,
            LocalDateTime abertoEm,
            String resolvidoPor,
            LocalDateTime resolvidoEm,
            String resolucao) {
    }

    public record InspectionDTO(
            Long id,
            Long transactionId,
            String tipo,
            String resultado,
            String inspetor,
            String observacoes,
            LocalDateTime executadoEm) {
    }

    public record AttachmentDTO(
            Long id,
            Long truckVisitId,
            Long transactionId,
            String categoria,
            String nomeArquivo,
            String contentType,
            String urlDocumento,
            Map<String, Object> metadados,
            String criadoPor,
            LocalDateTime criadoEm) {
    }

    public record DocumentDTO(
            Long id,
            Long truckVisitId,
            Long transactionId,
            String tipo,
            String numero,
            String status,
            Map<String, Object> conteudo,
            String emitidoPor,
            LocalDateTime emitidoEm,
            int reimpressoes,
            LocalDateTime ultimaReimpressaoEm) {
    }

    public record TransferDTO(
            Long id,
            String codigo,
            Long truckVisitId,
            Long facilityOrigemId,
            Long facilityDestinoId,
            String status,
            String solicitadoPor,
            LocalDateTime solicitadoEm,
            LocalDateTime recebidoEm,
            String observacoes) {
    }

    public record CapacityDTO(long janelas, long capacidade, long utilizada, long disponivel, BigDecimal ocupacaoPercentual) {
    }

    public record ReferenceCatalogDTO(
            List<BookingDTO> bookings,
            List<BillOfLadingDTO> billsOfLading,
            List<OrderDTO> ordens,
            List<PreadviceDTO> preAvisos) {

        public ReferenceCatalogDTO(List<BookingDTO> bookings, List<OrderDTO> ordens,
                                   List<PreadviceDTO> preAvisos) {
            this(bookings, List.of(), ordens, preAvisos);
        }
    }

    public record GateOperationalDashboardDTO(
            Long facilitySelecionadaId,
            List<FacilityDTO> facilities,
            List<GateDTO> gates,
            List<LaneDTO> lanes,
            List<StageDTO> stages,
            List<AccessRuleDTO> regrasAcesso,
            List<TruckVisitDTO> visitasAtivas,
            List<TroubleDTO> troublesAbertos,
            CapacityDTO capacidadeAgendamentos,
            ReferenceCatalogDTO referencias,
            LocalDateTime atualizadoEm) {

        public GateOperationalDashboardDTO(Long facilitySelecionadaId, List<FacilityDTO> facilities,
                                           List<GateDTO> gates, List<LaneDTO> lanes, List<StageDTO> stages,
                                           List<TruckVisitDTO> visitasAtivas, List<TroubleDTO> troublesAbertos,
                                           CapacityDTO capacidadeAgendamentos, ReferenceCatalogDTO referencias,
                                           LocalDateTime atualizadoEm) {
            this(facilitySelecionadaId, facilities, gates, lanes, stages, List.of(), visitasAtivas,
                    troublesAbertos, capacidadeAgendamentos, referencias, atualizadoEm);
        }
    }
}