package br.com.cloudport.servicoyard.ferrovia.dto;

import br.com.cloudport.servicoyard.ferrovia.modelo.StatusOperacaoConteinerVisita;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class OperacaoConteinerVisitaRequisicaoDto {

    @NotNull
    @Min(1)
    private Long idConteiner;

    private StatusOperacaoConteinerVisita statusOperacao = StatusOperacaoConteinerVisita.PENDENTE;

    public Long getIdConteiner() {
        return idConteiner;
    }

    public void setIdConteiner(Long idConteiner) {
        this.idConteiner = idConteiner;
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
