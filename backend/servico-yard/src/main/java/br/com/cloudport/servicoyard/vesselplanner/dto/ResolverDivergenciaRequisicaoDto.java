package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.vesselplanner.modelo.DecisaoResolucaoReconciliacao;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolverDivergenciaRequisicaoDto {

    @NotNull
    private DecisaoResolucaoReconciliacao decisao;

    @NotBlank
    @Size(max = 1000)
    private String justificativa;
}
