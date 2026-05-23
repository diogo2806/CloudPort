package br.com.cloudport.serviconavio.escala.listatrabalho.dto;

import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.StatusOrdemMovimentacaoNavio;
import javax.validation.constraints.NotNull;

public class AtualizacaoStatusOrdemNavioDTO {

    @NotNull(message = "Informe o status da movimentação.")
    private StatusOrdemMovimentacaoNavio statusMovimentacao;

    public StatusOrdemMovimentacaoNavio getStatusMovimentacao() {
        return statusMovimentacao;
    }

    public void setStatusMovimentacao(StatusOrdemMovimentacaoNavio statusMovimentacao) {
        this.statusMovimentacao = statusMovimentacao;
    }
}
