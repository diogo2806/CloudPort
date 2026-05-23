package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusOperacaoNavioConteiner;
import javax.validation.constraints.NotNull;

public class AtualizacaoStatusOperacaoDTO {

    @NotNull(message = "Informe o novo status da operação.")
    private StatusOperacaoNavioConteiner status;

    public StatusOperacaoNavioConteiner getStatus() {
        return status;
    }

    public void setStatus(StatusOperacaoNavioConteiner status) {
        this.status = status;
    }
}
