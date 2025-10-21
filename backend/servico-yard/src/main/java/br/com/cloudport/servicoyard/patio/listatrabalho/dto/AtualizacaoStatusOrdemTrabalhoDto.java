package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import javax.validation.constraints.NotNull;

public class AtualizacaoStatusOrdemTrabalhoDto {

    @NotNull
    private StatusOrdemTrabalhoPatio statusOrdem;

    public AtualizacaoStatusOrdemTrabalhoDto() {
    }

    public StatusOrdemTrabalhoPatio getStatusOrdem() {
        return statusOrdem;
    }

    public void setStatusOrdem(StatusOrdemTrabalhoPatio statusOrdem) {
        this.statusOrdem = statusOrdem;
    }
}
