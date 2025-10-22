package br.com.cloudport.serviconavio.navio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import br.com.cloudport.serviconavio.navio.entidade.StatusOperacaoNavio;

import java.time.LocalDateTime;

public class NavioDetalheDTO {

    private final Long identificador;
    private final String nome;
    private final String codigoImo;
    private final String paisBandeira;
    private final String empresaArmadora;
    private final Integer capacidadeTeu;
    private final StatusOperacaoNavio statusOperacao;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime dataPrevistaAtracacao;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime dataEfetivaAtracacao;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime dataEfetivaDesatracacao;
    private final String bercoPrevisto;
    private final String bercoAtual;
    private final String observacoes;

    public NavioDetalheDTO(Long identificador,
                           String nome,
                           String codigoImo,
                           String paisBandeira,
                           String empresaArmadora,
                           Integer capacidadeTeu,
                           StatusOperacaoNavio statusOperacao,
                           LocalDateTime dataPrevistaAtracacao,
                           LocalDateTime dataEfetivaAtracacao,
                           LocalDateTime dataEfetivaDesatracacao,
                           String bercoPrevisto,
                           String bercoAtual,
                           String observacoes) {
        this.identificador = identificador;
        this.nome = nome;
        this.codigoImo = codigoImo;
        this.paisBandeira = paisBandeira;
        this.empresaArmadora = empresaArmadora;
        this.capacidadeTeu = capacidadeTeu;
        this.statusOperacao = statusOperacao;
        this.dataPrevistaAtracacao = dataPrevistaAtracacao;
        this.dataEfetivaAtracacao = dataEfetivaAtracacao;
        this.dataEfetivaDesatracacao = dataEfetivaDesatracacao;
        this.bercoPrevisto = bercoPrevisto;
        this.bercoAtual = bercoAtual;
        this.observacoes = observacoes;
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

    public StatusOperacaoNavio getStatusOperacao() {
        return statusOperacao;
    }

    public LocalDateTime getDataPrevistaAtracacao() {
        return dataPrevistaAtracacao;
    }

    public LocalDateTime getDataEfetivaAtracacao() {
        return dataEfetivaAtracacao;
    }

    public LocalDateTime getDataEfetivaDesatracacao() {
        return dataEfetivaDesatracacao;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
