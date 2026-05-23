package br.com.cloudport.serviconavio.navio.dto;

import java.math.BigDecimal;

public class NavioDetalheDTO {

    private final Long identificador;
    private final String nome;
    private final String codigoImo;
    private final String paisBandeira;
    private final String empresaArmadora;
    private final Integer capacidadeTeu;
    private final BigDecimal loaMetros;
    private final BigDecimal caladoMaximoMetros;
    private final String callSign;

    public NavioDetalheDTO(Long identificador,
                           String nome,
                           String codigoImo,
                           String paisBandeira,
                           String empresaArmadora,
                           Integer capacidadeTeu,
                           BigDecimal loaMetros,
                           BigDecimal caladoMaximoMetros,
                           String callSign) {
        this.identificador = identificador;
        this.nome = nome;
        this.codigoImo = codigoImo;
        this.paisBandeira = paisBandeira;
        this.empresaArmadora = empresaArmadora;
        this.capacidadeTeu = capacidadeTeu;
        this.loaMetros = loaMetros;
        this.caladoMaximoMetros = caladoMaximoMetros;
        this.callSign = callSign;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public String getPaisBandeira() {
        return paisBandeira;
    }

    public String getEmpresaArmadora() {
        return empresaArmadora;
    }

    public Integer getCapacidadeTeu() {
        return capacidadeTeu;
    }

    public BigDecimal getLoaMetros() {
        return loaMetros;
    }

    public BigDecimal getCaladoMaximoMetros() {
        return caladoMaximoMetros;
    }

    public String getCallSign() {
        return callSign;
    }
}
