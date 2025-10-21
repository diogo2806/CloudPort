package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import org.springframework.web.util.HtmlUtils;

public class OperacaoConteinerVisitaRespostaDto {

    private final String codigoConteiner;
    private final StatusOperacaoConteinerVisita statusOperacao;

    public OperacaoConteinerVisitaRespostaDto(String codigoConteiner,
                                              StatusOperacaoConteinerVisita statusOperacao) {
        this.codigoConteiner = codigoConteiner;
        this.statusOperacao = statusOperacao;
    }

    public static OperacaoConteinerVisitaRespostaDto deEmbeddable(OperacaoConteinerVisita operacao) {
        return new OperacaoConteinerVisitaRespostaDto(
                HtmlUtils.htmlEscape(operacao.getCodigoConteiner()),
                operacao.getStatusOperacao()
        );
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public StatusOperacaoConteinerVisita getStatusOperacao() {
        return statusOperacao;
    }
}
