package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.StatusOperacaoConteinerEscala;
import javax.validation.constraints.NotNull;

public class AtualizacaoStatusConteinerEscalaDTO {

    @NotNull(message = "Informe o status da operação do contêiner.")
    private StatusOperacaoConteinerEscala statusOperacao;

    public StatusOperacaoConteinerEscala getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoConteinerEscala statusOperacao) {
        this.statusOperacao = statusOperacao;
    }
}
