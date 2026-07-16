package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AtualizacaoPrioridadeOrdemTrabalhoDto extends ComandoMotivadoDto {

    @NotNull
    @Min(0)
    private Integer prioridadeOperacional;

    private Boolean prioridadeBusca;

    public AtualizacaoPrioridadeOrdemTrabalhoDto() {
    }

    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    public Boolean getPrioridadeBusca() { return prioridadeBusca; }
    public void setPrioridadeBusca(Boolean prioridadeBusca) { this.prioridadeBusca = prioridadeBusca; }

    public boolean prioridadeBuscaEfetiva() {
        return Boolean.TRUE.equals(prioridadeBusca);
    }
}
