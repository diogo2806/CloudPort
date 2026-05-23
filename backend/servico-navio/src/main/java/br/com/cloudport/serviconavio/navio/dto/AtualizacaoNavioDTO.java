package br.com.cloudport.serviconavio.navio.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class AtualizacaoNavioDTO {

    @Size(max = 120, message = "O nome do navio deve ter no máximo 120 caracteres.")
    private String nome;

    @Pattern(regexp = "^IMO[0-9]{7}$", message = "O código IMO deve seguir o padrão IMO9999999.")
    private String codigoImo;

    @Size(max = 60, message = "O país da bandeira deve ter no máximo 60 caracteres.")
    private String paisBandeira;

    @Size(max = 80, message = "A empresa armadora deve ter no máximo 80 caracteres.")
    private String empresaArmadora;

    @Positive(message = "A capacidade em TEU deve ser maior que zero.")
    private Integer capacidadeTeu;

    @DecimalMin(value = "0.0", inclusive = false, message = "O comprimento (LOA) deve ser maior que zero.")
    @Digits(integer = 4, fraction = 2, message = "O comprimento (LOA) deve ter no máximo 4 dígitos inteiros e 2 decimais.")
    private BigDecimal loaMetros;

    @DecimalMin(value = "0.0", inclusive = false, message = "O calado máximo deve ser maior que zero.")
    @Digits(integer = 3, fraction = 2, message = "O calado máximo deve ter no máximo 3 dígitos inteiros e 2 decimais.")
    private BigDecimal caladoMaximoMetros;

    @Size(max = 15, message = "O call sign deve ter no máximo 15 caracteres.")
    private String callSign;

    public AtualizacaoNavioDTO() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public void setCodigoImo(String codigoImo) {
        this.codigoImo = codigoImo;
    }

    public String getPaisBandeira() {
        return paisBandeira;
    }

    public void setPaisBandeira(String paisBandeira) {
        this.paisBandeira = paisBandeira;
    }

    public String getEmpresaArmadora() {
        return empresaArmadora;
    }

    public void setEmpresaArmadora(String empresaArmadora) {
        this.empresaArmadora = empresaArmadora;
    }

    public Integer getCapacidadeTeu() {
        return capacidadeTeu;
    }

    public void setCapacidadeTeu(Integer capacidadeTeu) {
        this.capacidadeTeu = capacidadeTeu;
    }

    public BigDecimal getLoaMetros() {
        return loaMetros;
    }

    public void setLoaMetros(BigDecimal loaMetros) {
        this.loaMetros = loaMetros;
    }

    public BigDecimal getCaladoMaximoMetros() {
        return caladoMaximoMetros;
    }

    public void setCaladoMaximoMetros(BigDecimal caladoMaximoMetros) {
        this.caladoMaximoMetros = caladoMaximoMetros;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }
}
