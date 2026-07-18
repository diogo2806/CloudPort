package br.com.cloudport.servicoyard.patio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReeferTelemetriaPatioDto {

    private final Long conteinerId;
    private final String codigoConteiner;
    private final String bloco;
    private final Integer linha;
    private final Integer coluna;
    private final String camadaOperacional;
    private final BigDecimal temperaturaAtualCelsius;
    private final BigDecimal temperaturaMinimaCelsius;
    private final BigDecimal temperaturaMaximaCelsius;
    private final boolean ligado;
    private final LocalDateTime registradoEm;
    private final String statusAlarme;
    private final String mensagemAlarme;

    public ReeferTelemetriaPatioDto(Long conteinerId,
                                    String codigoConteiner,
                                    String bloco,
                                    Integer linha,
                                    Integer coluna,
                                    String camadaOperacional,
                                    BigDecimal temperaturaAtualCelsius,
                                    BigDecimal temperaturaMinimaCelsius,
                                    BigDecimal temperaturaMaximaCelsius,
                                    boolean ligado,
                                    LocalDateTime registradoEm,
                                    String statusAlarme,
                                    String mensagemAlarme) {
        this.conteinerId = conteinerId;
        this.codigoConteiner = codigoConteiner;
        this.bloco = bloco;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
        this.temperaturaAtualCelsius = temperaturaAtualCelsius;
        this.temperaturaMinimaCelsius = temperaturaMinimaCelsius;
        this.temperaturaMaximaCelsius = temperaturaMaximaCelsius;
        this.ligado = ligado;
        this.registradoEm = registradoEm;
        this.statusAlarme = statusAlarme;
        this.mensagemAlarme = mensagemAlarme;
    }

    public Long getConteinerId() { return conteinerId; }
    public String getCodigoConteiner() { return codigoConteiner; }
    public String getBloco() { return bloco; }
    public Integer getLinha() { return linha; }
    public Integer getColuna() { return coluna; }
    public String getCamadaOperacional() { return camadaOperacional; }
    public BigDecimal getTemperaturaAtualCelsius() { return temperaturaAtualCelsius; }
    public BigDecimal getTemperaturaMinimaCelsius() { return temperaturaMinimaCelsius; }
    public BigDecimal getTemperaturaMaximaCelsius() { return temperaturaMaximaCelsius; }
    public boolean isLigado() { return ligado; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public String getStatusAlarme() { return statusAlarme; }
    public String getMensagemAlarme() { return mensagemAlarme; }
}