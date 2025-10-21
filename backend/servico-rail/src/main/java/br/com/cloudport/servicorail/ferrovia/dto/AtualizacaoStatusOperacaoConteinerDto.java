package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import javax.validation.constraints.NotNull;

public class AtualizacaoStatusOperacaoConteinerDto {

    @NotNull
    private StatusOperacaoConteinerVisita statusOperacao;

    public StatusOperacaoConteinerVisita getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoConteinerVisita statusOperacao) {
        this.statusOperacao = statusOperacao;
    }
}
