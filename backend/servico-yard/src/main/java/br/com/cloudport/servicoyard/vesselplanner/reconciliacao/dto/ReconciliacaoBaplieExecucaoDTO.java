package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto;

import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.DecisaoResolucao;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public final class ReconciliacaoBaplieExecucaoDTO {

    private ReconciliacaoBaplieExecucaoDTO() {
    }

    public record ReconciliarRequest(String usuario) {
    }

    public record ResolverDivergenciaRequest(
            @NotNull DecisaoResolucao decisao,
            @NotBlank String motivo,
            @NotBlank String usuario) {
    }

    public record DivergenciaResposta(
            Long id,
            Long slotNavioId,
            String codigoContainer,
            String tipo,
            String severidade,
            String status,
            String campo,
            String fonteReferencia,
            String valorReferencia,
            String fonteDivergente,
            String valorDivergente,
            String decisaoResolucao,
            String motivoResolucao,
            String responsavelResolucao,
            LocalDateTime detectadaEm,
            LocalDateTime resolvidaEm) {
    }

    public record ReconciliacaoResposta(
            Long id,
            Long planoId,
            Long bayPlanId,
            Long visitaNavioId,
            Long versaoPlano,
            String status,
            int totalUnidades,
            int totalDivergencias,
            int totalCriticasAbertas,
            boolean bloqueiaOperacao,
            String solicitante,
            LocalDateTime executadaEm,
            LocalDateTime concluidaEm,
            List<DivergenciaResposta> divergencias) {
    }
}
