package br.com.cloudport.servicorail.ferrovia.modelo;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class VagaoVisita {

    @Column(name = "posicao_no_trem", nullable = false)
    private Integer posicaoNoTrem;

    @Column(name = "identificador_vagao", nullable = false, length = 35)
    private String identificadorVagao;

    @Column(name = "tipo_vagao", length = 40)
    private String tipoVagao;

    public VagaoVisita() {
    }

    public VagaoVisita(Integer posicaoNoTrem, String identificadorVagao, String tipoVagao) {
        this.posicaoNoTrem = posicaoNoTrem;
        this.identificadorVagao = identificadorVagao;
        this.tipoVagao = tipoVagao;
    }

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
        this.identificadorVagao = identificadorVagao;
    }

    public String getTipoVagao() {
        return tipoVagao;
    }

    public void setTipoVagao(String tipoVagao) {
        this.tipoVagao = tipoVagao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VagaoVisita that = (VagaoVisita) o;
        return Objects.equals(posicaoNoTrem, that.posicaoNoTrem)
                && Objects.equals(identificadorVagao, that.identificadorVagao)
                && Objects.equals(tipoVagao, that.tipoVagao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(posicaoNoTrem, identificadorVagao, tipoVagao);
    }
}
