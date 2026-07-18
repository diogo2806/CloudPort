package br.com.cloudport.servicoyard.vesselplanner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public final class OperacaoTampaPoraoDTOs {

    private OperacaoTampaPoraoDTOs() {
    }

    @Getter
    @Setter
    @Schema(name = "TampaPoraoResposta")
    public static class TampaPoraoResposta {
        private Long id;
        private String codigo;
        private int bayInicial;
        private int bayFinal;
        private String posicao;
        private String recursoAtual;
        private boolean bloqueioAtivo;
        private List<TarefaTampaPoraoResposta> tarefas = new ArrayList<>();
    }

    @Getter
    @Setter
    @Schema(name = "TarefaTampaPoraoResposta")
    public static class TarefaTampaPoraoResposta {
        private Long id;
        private String tipo;
        private String status;
        private int ordemOperacional;
        private Integer ordemMovimentoReferencia;
        private String momentoSequencia;
        private Long dependenciaId;
        private String recurso;
        private String iniciadoPor;
        private String confirmadoPor;
        private String canceladoPor;
        private String observacao;
        private LocalDateTime iniciadoEm;
        private LocalDateTime confirmadoEm;
        private LocalDateTime canceladoEm;
    }

    @Getter
    @Setter
    @Schema(name = "IniciarTarefaTampaPoraoRequest")
    public static class IniciarTarefaRequest {
        @NotBlank
        @Size(max = 80)
        private String recurso;

        @Size(max = 500)
        private String observacao;
    }

    @Getter
    @Setter
    @Schema(name = "ConfirmarTarefaTampaPoraoRequest")
    public static class ConfirmarTarefaRequest {
        @Size(max = 500)
        private String observacao;
    }

    @Getter
    @Setter
    @Schema(name = "CancelarTarefaTampaPoraoRequest")
    public static class CancelarTarefaRequest {
        @NotBlank
        @Size(max = 500)
        private String motivo;
    }

    @Getter
    @Setter
    @Schema(name = "IniciarMovimentoContainerNavioRequest")
    public static class IniciarMovimentoRequest {
        @Min(1)
        private int ordemSequencia;

        @NotBlank
        @Size(max = 20)
        private String tipoOperacao;

        @Min(1)
        private int guindasteId;
    }

    @Getter
    @Setter
    @Schema(name = "ConcluirMovimentoContainerNavioRequest")
    public static class ConcluirMovimentoRequest {
        @Size(max = 500)
        private String observacao;
    }

    @Getter
    @Setter
    @Schema(name = "MovimentoContainerNavioResposta")
    public static class MovimentoResposta {
        private Long id;
        private Long planoId;
        private Long slotId;
        private int ordemSequencia;
        private String codigoContainer;
        private String tipoOperacao;
        private int guindasteId;
        private String status;
        private String iniciadoPor;
        private String concluidoPor;
        private String observacao;
        private LocalDateTime iniciadoEm;
        private LocalDateTime concluidoEm;
    }
}
