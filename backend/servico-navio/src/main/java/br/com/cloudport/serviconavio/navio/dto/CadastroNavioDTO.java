package br.com.cloudport.serviconavio.navio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class CadastroNavioDTO {

    @NotBlank(message = "Informe o nome do navio.")
    @Size(max = 120, message = "O nome do navio deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o código IMO do navio.")
    @Pattern(regexp = "^IMO[0-9]{7}$", message = "O código IMO deve seguir o padrão IMO9999999.")
    private String codigoImo;

    @NotBlank(message = "Informe o país da bandeira.")
    @Size(max = 60, message = "O país da bandeira deve ter no máximo 60 caracteres.")
    private String paisBandeira;

    @NotBlank(message = "Informe a empresa armadora.")
    @Size(max = 80, message = "A empresa armadora deve ter no máximo 80 caracteres.")
    private String empresaArmadora;

    @NotNull(message = "Informe a capacidade em TEU.")
    @Positive(message = "A capacidade em TEU deve ser maior que zero.")
    private Integer capacidadeTeu;

    @NotNull(message = "Informe a data prevista de atracação.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataPrevistaAtracacao;

    @Size(max = 20, message = "O berço previsto deve ter no máximo 20 caracteres.")
    private String bercoPrevisto;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;

    public CadastroNavioDTO() {
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

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public void setBercoPrevisto(String bercoPrevisto) {
        this.bercoPrevisto = bercoPrevisto;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
