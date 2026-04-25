package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import org.springframework.web.util.HtmlUtils;

public class OperacaoConteinerVisitaRespostaDto {

    private final String codigoConteiner;
    private final StatusOperacaoConteinerVisita statusOperacao;
    private final String identificadorVagao;

    public OperacaoConteinerVisitaRespostaDto(String codigoConteiner,
                                              StatusOperacaoConteinerVisita statusOperacao,
                                              String identificadorVagao) {
        this.codigoConteiner = codigoConteiner;
        this.statusOperacao = statusOperacao;
        this.identificadorVagao = identificadorVagao;
    }

    public static OperacaoConteinerVisitaRespostaDto deEmbeddable(OperacaoConteinerVisita operacao) {
        return new OperacaoConteinerVisitaRespostaDto(
                HtmlUtils.htmlEscape(operacao.getCodigoConteiner()),
                operacao.getStatusOperacao(),
                HtmlUtils.htmlEscape(operacao.getIdentificadorVagao())
        );
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public StatusOperacaoConteinerVisita getStatusOperacao() {
        return statusOperacao;
    }

    public String getIdentificadorVagao() {
        return identificadorVagao;
    }
}
