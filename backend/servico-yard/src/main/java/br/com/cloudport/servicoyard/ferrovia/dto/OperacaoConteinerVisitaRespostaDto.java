package br.com.cloudport.servicoyard.ferrovia.dto;

import br.com.cloudport.servicoyard.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicoyard.ferrovia.modelo.StatusOperacaoConteinerVisita;

public class OperacaoConteinerVisitaRespostaDto {

    private final Long idConteiner;
    private final StatusOperacaoConteinerVisita statusOperacao;

    public OperacaoConteinerVisitaRespostaDto(Long idConteiner,
                                              StatusOperacaoConteinerVisita statusOperacao) {
        this.idConteiner = idConteiner;
        this.statusOperacao = statusOperacao;
    }

    public static OperacaoConteinerVisitaRespostaDto deEmbeddable(OperacaoConteinerVisita operacao) {
        return new OperacaoConteinerVisitaRespostaDto(operacao.getIdConteiner(), operacao.getStatusOperacao());
    }

    public Long getIdConteiner() {
        return idConteiner;
    }

    public StatusOperacaoConteinerVisita getStatusOperacao() {
        return statusOperacao;
    }
}
