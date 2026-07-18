package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconciliacaoBaplieExecucaoDto {

    private Long planoId;
    private Long bayPlanId;
    private LocalDateTime reconciliadoEm;
    private int totalDivergencias;
    private int abertas;
    private int criticasAbertas;
    private int resolvidas;
    private List<DivergenciaReconciliacaoDto> divergencias = new ArrayList<>();
}
