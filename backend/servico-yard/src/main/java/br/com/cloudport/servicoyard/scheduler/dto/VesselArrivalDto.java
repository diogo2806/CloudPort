package br.com.cloudport.servicoyard.scheduler.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class VesselArrivalDto {

    @NotBlank
    private String codigoNavio;

    @NotBlank
    private String nomeBerco;

    @NotNull
    private LocalDateTime etaChegada;

    @NotNull
    private LocalDateTime etaPartida;

    @Positive
    private Integer quantidadeContainersImportacao;

    @Positive
    private Integer quantidadeContainersExportacao;

    private String prioridade;

    private String observacoes;

    public VesselArrivalDto() {
    }

    public VesselArrivalDto(String codigoNavio, String nomeBerco, LocalDateTime etaChegada,
                           LocalDateTime etaPartida, Integer quantidadeContainersImportacao,
                           Integer quantidadeContainersExportacao) {
        this.codigoNavio = codigoNavio;
        this.nomeBerco = nomeBerco;
        this.etaChegada = etaChegada;
        this.etaPartida = etaPartida;
        this.quantidadeContainersImportacao = quantidadeContainersImportacao;
        this.quantidadeContainersExportacao = quantidadeContainersExportacao;
    }

    public String getCodigoNavio() {
        return codigoNavio;
    }

    public void setCodigoNavio(String codigoNavio) {
        this.codigoNavio = codigoNavio;
    }

    public String getNomeBerco() {
        return nomeBerco;
    }

    public void setNomeBerco(String nomeBerco) {
        this.nomeBerco = nomeBerco;
    }

    public LocalDateTime getEtaChegada() {
        return etaChegada;
    }

    public void setEtaChegada(LocalDateTime etaChegada) {
        this.etaChegada = etaChegada;
    }

    public LocalDateTime getEtaPartida() {
        return etaPartida;
    }

    public void setEtaPartida(LocalDateTime etaPartida) {
        this.etaPartida = etaPartida;
    }

    public Integer getQuantidadeContainersImportacao() {
        return quantidadeContainersImportacao;
    }

    public void setQuantidadeContainersImportacao(Integer quantidadeContainersImportacao) {
        this.quantidadeContainersImportacao = quantidadeContainersImportacao;
    }

    public Integer getQuantidadeContainersExportacao() {
        return quantidadeContainersExportacao;
    }

    public void setQuantidadeContainersExportacao(Integer quantidadeContainersExportacao) {
        this.quantidadeContainersExportacao = quantidadeContainersExportacao;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Integer getJanelaTempoHoras() {
        if (etaChegada != null && etaPartida != null) {
            return (int) java.time.temporal.ChronoUnit.HOURS.between(etaChegada, etaPartida);
        }
        return 0;
    }
}
