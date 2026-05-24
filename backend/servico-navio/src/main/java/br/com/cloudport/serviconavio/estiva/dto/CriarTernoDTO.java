package br.com.cloudport.serviconavio.estiva.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CriarTernoDTO {

    @NotBlank(message = "Informe o identificador do terno.")
    @Size(max = 40, message = "O identificador do terno deve ter no máximo 40 caracteres.")
    private String identificador;

    @NotNull(message = "Informe a sequência de operação do terno.")
    @Min(value = 1, message = "A sequência deve ser maior ou igual a 1.")
    private Integer sequencia;

    @NotNull(message = "Informe a baia inicial do terno.")
    @Min(value = 1, message = "A baia inicial deve ser maior ou igual a 1.")
    private Integer baiaInicial;

    @NotNull(message = "Informe a baia final do terno.")
    @Min(value = 1, message = "A baia final deve ser maior ou igual a 1.")
    private Integer baiaFinal;

    public CriarTernoDTO() {
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public Integer getBaiaInicial() {
        return baiaInicial;
    }

    public void setBaiaInicial(Integer baiaInicial) {
        this.baiaInicial = baiaInicial;
    }

    public Integer getBaiaFinal() {
        return baiaFinal;
    }

    public void setBaiaFinal(Integer baiaFinal) {
        this.baiaFinal = baiaFinal;
    }
}
