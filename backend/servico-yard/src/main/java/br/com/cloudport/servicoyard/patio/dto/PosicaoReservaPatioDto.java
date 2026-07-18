package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import java.math.BigDecimal;
import java.util.List;

public class PosicaoReservaPatioDto {

    private final Long id;
    private final Integer linha;
    private final Integer coluna;
    private final String camadaOperacional;
    private final boolean ocupada;
    private final String codigoConteiner;
    private final StatusConteiner statusConteiner;
    private final String bloco;
    private final boolean bloqueada;
    private final boolean interditada;
    private final boolean areaPermitida;
    private final String notaOperacional;
    private final List<String> tiposCargaPermitidos;
    private final BigDecimal pesoMaximoToneladas;
    private final BigDecimal alturaMaximaMetros;
    private final Integer camadaMaxima;
    private final Integer capacidadePilha;
    private final long ocupacaoPilha;

    public PosicaoReservaPatioDto(Long id,
                                  Integer linha,
                                  Integer coluna,
                                  String camadaOperacional,
                                  boolean ocupada,
                                  String codigoConteiner,
                                  StatusConteiner statusConteiner,
                                  String bloco,
                                  boolean bloqueada,
                                  boolean interditada,
                                  boolean areaPermitida,
                                  String notaOperacional,
                                  List<String> tiposCargaPermitidos,
                                  BigDecimal pesoMaximoToneladas,
                                  BigDecimal alturaMaximaMetros,
                                  Integer camadaMaxima,
                                  Integer capacidadePilha,
                                  long ocupacaoPilha) {
        this.id = id;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
        this.ocupada = ocupada;
        this.codigoConteiner = codigoConteiner;
        this.statusConteiner = statusConteiner;
        this.bloco = bloco;
        this.bloqueada = bloqueada;
        this.interditada = interditada;
        this.areaPermitida = areaPermitida;
        this.notaOperacional = notaOperacional;
        this.tiposCargaPermitidos = tiposCargaPermitidos;
        this.pesoMaximoToneladas = pesoMaximoToneladas;
        this.alturaMaximaMetros = alturaMaximaMetros;
        this.camadaMaxima = camadaMaxima;
        this.capacidadePilha = capacidadePilha;
        this.ocupacaoPilha = ocupacaoPilha;
    }

    public Long getId() {
        return id;
    }

    public Integer getLinha() {
        return linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public boolean isOcupada() {
        return ocupada;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public StatusConteiner getStatusConteiner() {
        return statusConteiner;
    }

    public String getBloco() {
        return bloco;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public boolean isInterditada() {
        return interditada;
    }

    public boolean isAreaPermitida() {
        return areaPermitida;
    }

    public String getNotaOperacional() {
        return notaOperacional;
    }

    public List<String> getTiposCargaPermitidos() {
        return tiposCargaPermitidos;
    }

    public BigDecimal getPesoMaximoToneladas() {
        return pesoMaximoToneladas;
    }

    public BigDecimal getAlturaMaximaMetros() {
        return alturaMaximaMetros;
    }

    public Integer getCamadaMaxima() {
        return camadaMaxima;
    }

    public Integer getCapacidadePilha() {
        return capacidadePilha;
    }

    public long getOcupacaoPilha() {
        return ocupacaoPilha;
    }
}