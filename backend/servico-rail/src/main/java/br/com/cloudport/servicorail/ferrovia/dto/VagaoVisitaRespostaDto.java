package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import org.springframework.web.util.HtmlUtils;

public class VagaoVisitaRespostaDto {

    private final Integer posicaoNoTrem;
    private final String identificadorVagao;
    private final String tipoVagao;
    private final Integer capacidadeConteineres;

    public VagaoVisitaRespostaDto(Integer posicaoNoTrem,
                                  String identificadorVagao,
                                  String tipoVagao,
                                  Integer capacidadeConteineres) {
        this.posicaoNoTrem = posicaoNoTrem;
        this.identificadorVagao = identificadorVagao;
        this.tipoVagao = tipoVagao;
        this.capacidadeConteineres = capacidadeConteineres;
    }

    public static VagaoVisitaRespostaDto deEmbeddable(VagaoVisita vagao) {
        return new VagaoVisitaRespostaDto(
                vagao.getPosicaoNoTrem(),
                HtmlUtils.htmlEscape(vagao.getIdentificadorVagao()),
                HtmlUtils.htmlEscape(vagao.getTipoVagao()),
                vagao.getCapacidadeConteineres()
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

    public Integer getCapacidadeConteineres() {
        return capacidadeConteineres;
    }
}
