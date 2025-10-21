package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import org.springframework.web.util.HtmlUtils;

public class VagaoVisitaRespostaDto {

    private final Integer posicaoNoTrem;
    private final String identificadorVagao;
    private final String tipoVagao;

    public VagaoVisitaRespostaDto(Integer posicaoNoTrem, String identificadorVagao, String tipoVagao) {
        this.posicaoNoTrem = posicaoNoTrem;
        this.identificadorVagao = identificadorVagao;
        this.tipoVagao = tipoVagao;
    }

    public static VagaoVisitaRespostaDto deEmbeddable(VagaoVisita vagao) {
        return new VagaoVisitaRespostaDto(
                vagao.getPosicaoNoTrem(),
                HtmlUtils.htmlEscape(vagao.getIdentificadorVagao()),
                HtmlUtils.htmlEscape(vagao.getTipoVagao())
        );
    }

    public Integer getPosicaoNoTrem() {
        return posicaoNoTrem;
    }

    public String getIdentificadorVagao() {
        return identificadorVagao;
    }

    public String getTipoVagao() {
        return tipoVagao;
    }
}
