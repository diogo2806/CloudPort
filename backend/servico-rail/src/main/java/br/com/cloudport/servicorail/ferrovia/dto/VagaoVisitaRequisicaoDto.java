package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class VagaoVisitaRequisicaoDto {

    @NotNull
    @Min(1)
    private Integer posicaoNoTrem;

    @NotBlank
    @Size(max = 35)
    private String identificadorVagao;

    @Size(max = 40)
    private String tipoVagao;

    public Integer getPosicaoNoTrem() {
        return posicaoNoTrem;
    }

    public void setPosicaoNoTrem(Integer posicaoNoTrem) {
        this.posicaoNoTrem = posicaoNoTrem;
    }

    public String getIdentificadorVagao() {
        return identificadorVagao;
    }

    public void setIdentificadorVagao(String identificadorVagao) {
        this.identificadorVagao = ValidacaoEntradaUtil.limparTexto(identificadorVagao);
    }

    public String getTipoVagao() {
        return tipoVagao;
    }

    public void setTipoVagao(String tipoVagao) {
        this.tipoVagao = ValidacaoEntradaUtil.limparTexto(tipoVagao);
    }
}
