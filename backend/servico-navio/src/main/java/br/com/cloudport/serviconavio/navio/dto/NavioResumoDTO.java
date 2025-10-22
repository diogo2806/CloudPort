package br.com.cloudport.serviconavio.navio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import br.com.cloudport.serviconavio.navio.entidade.StatusOperacaoNavio;

import java.time.LocalDateTime;

public class NavioResumoDTO {

    private final Long identificador;
    private final String nome;
    private final String codigoImo;
    private final StatusOperacaoNavio statusOperacao;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime dataPrevistaAtracacao;
    private final String bercoPrevisto;

    public NavioResumoDTO(Long identificador,
                          String nome,
                          String codigoImo,
                          StatusOperacaoNavio statusOperacao,
                          LocalDateTime dataPrevistaAtracacao,
                          String bercoPrevisto) {
        this.identificador = identificador;
        this.nome = nome;
        this.codigoImo = codigoImo;
        this.statusOperacao = statusOperacao;
        this.dataPrevistaAtracacao = dataPrevistaAtracacao;
        this.bercoPrevisto = bercoPrevisto;
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

    public StatusOperacaoNavio getStatusOperacao() {
        return statusOperacao;
    }

    public LocalDateTime getDataPrevistaAtracacao() {
        return dataPrevistaAtracacao;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }
}
