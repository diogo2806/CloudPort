package br.com.cloudport.servicorail.ferrovia.listatrabalho.dto;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import javax.validation.constraints.NotNull;

public class AtualizacaoStatusOrdemMovimentacaoDto {

    @NotNull
    private StatusOrdemMovimentacao statusMovimentacao;

    public StatusOrdemMovimentacao getStatusMovimentacao() {
        return statusMovimentacao;
    }

    public void setStatusMovimentacao(StatusOrdemMovimentacao statusMovimentacao) {
        this.statusMovimentacao = statusMovimentacao;
    }
}
