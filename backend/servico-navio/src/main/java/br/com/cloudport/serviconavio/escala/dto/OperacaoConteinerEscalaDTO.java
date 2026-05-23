package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.StatusOperacaoConteinerEscala;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class OperacaoConteinerEscalaDTO {

    @NotBlank(message = "Informe o código do contêiner.")
    @Size(max = 20, message = "O código do contêiner deve ter no máximo 20 caracteres.")
    private String codigoConteiner;

    private StatusOperacaoConteinerEscala statusOperacao;

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public StatusOperacaoConteinerEscala getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoConteinerEscala statusOperacao) {
        this.statusOperacao = statusOperacao;
    }
}
