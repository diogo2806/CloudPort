package br.com.cloudport.serviconavio.navio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import br.com.cloudport.serviconavio.navio.entidade.StatusOperacaoNavio;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataPrevistaAtracacao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataEfetivaAtracacao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataEfetivaDesatracacao;

    @Size(max = 20, message = "O berço previsto deve ter no máximo 20 caracteres.")
    private String bercoPrevisto;

    @Size(max = 20, message = "O berço atual deve ter no máximo 20 caracteres.")
    private String bercoAtual;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;

    @NotNull(message = "Informe o status da operação do navio.")
    private StatusOperacaoNavio statusOperacao;

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

    public LocalDateTime getDataPrevistaAtracacao() {
        return dataPrevistaAtracacao;
    }

    public void setDataPrevistaAtracacao(LocalDateTime dataPrevistaAtracacao) {
        this.dataPrevistaAtracacao = dataPrevistaAtracacao;
    }

    public LocalDateTime getDataEfetivaAtracacao() {
        return dataEfetivaAtracacao;
    }

    public void setDataEfetivaAtracacao(LocalDateTime dataEfetivaAtracacao) {
        this.dataEfetivaAtracacao = dataEfetivaAtracacao;
    }

    public LocalDateTime getDataEfetivaDesatracacao() {
        return dataEfetivaDesatracacao;
    }

    public void setDataEfetivaDesatracacao(LocalDateTime dataEfetivaDesatracacao) {
        this.dataEfetivaDesatracacao = dataEfetivaDesatracacao;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public void setBercoPrevisto(String bercoPrevisto) {
        this.bercoPrevisto = bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public void setBercoAtual(String bercoAtual) {
        this.bercoAtual = bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public StatusOperacaoNavio getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoNavio statusOperacao) {
        this.statusOperacao = statusOperacao;
    }
}
