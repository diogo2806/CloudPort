package br.com.cloudport.servicoyard.container.dto;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class AtualizacaoConteinerDTO {
    @NotBlank(message = "Posição no pátio obrigatória")
    @Size(max = 60, message = "Posição no pátio deve ter até 60 caracteres")
    private String posicaoPatio;

    @NotNull(message = "Tipo de carga obrigatório")
    private TipoCargaConteiner tipoCarga;

    @NotNull(message = "Peso obrigatório")
    @DecimalMin(value = "0.1", message = "Peso mínimo de 0.1 tonelada")
    @DecimalMax(value = "120.0", message = "Peso máximo de 120 toneladas")
    private BigDecimal pesoToneladas;

    @Size(max = 255, message = "Restrições devem ter até 255 caracteres")
    private String restricoes;

    public AtualizacaoConteinerDTO() {
    }

    public String getPosicaoPatio() {
        return posicaoPatio;
    }

    public void setPosicaoPatio(String posicaoPatio) {
        this.posicaoPatio = posicaoPatio;
    }

    public TipoCargaConteiner getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(TipoCargaConteiner tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }

    public String getRestricoes() {
        return restricoes;
    }

    public void setRestricoes(String restricoes) {
        this.restricoes = restricoes;
    }
}
