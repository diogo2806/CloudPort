package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.OperacaoConteinerEscala;
import br.com.cloudport.serviconavio.escala.entidade.StatusOperacaoConteinerEscala;
import org.springframework.web.util.HtmlUtils;

public class OperacaoConteinerEscalaResumoDTO {

    private final String codigoConteiner;
    private final StatusOperacaoConteinerEscala statusOperacao;

    public OperacaoConteinerEscalaResumoDTO(String codigoConteiner, StatusOperacaoConteinerEscala statusOperacao) {
        this.codigoConteiner = codigoConteiner;
        this.statusOperacao = statusOperacao;
    }

    public static OperacaoConteinerEscalaResumoDTO deEntidade(OperacaoConteinerEscala operacao) {
        return new OperacaoConteinerEscalaResumoDTO(
                HtmlUtils.htmlEscape(operacao.getCodigoConteiner()),
                operacao.getStatusOperacao()
        );
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public StatusOperacaoConteinerEscala getStatusOperacao() {
        return statusOperacao;
    }
}
