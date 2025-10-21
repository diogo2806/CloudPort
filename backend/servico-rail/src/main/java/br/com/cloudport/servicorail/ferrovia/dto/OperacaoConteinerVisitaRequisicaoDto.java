package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class OperacaoConteinerVisitaRequisicaoDto {

    @NotBlank
    @Size(max = 20)
    private String codigoConteiner;

    private StatusOperacaoConteinerVisita statusOperacao = StatusOperacaoConteinerVisita.PENDENTE;

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public StatusOperacaoConteinerVisita getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoConteinerVisita statusOperacao) {
        if (statusOperacao != null) {
            this.statusOperacao = statusOperacao;
        }
    }
}
