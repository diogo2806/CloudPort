package br.com.cloudport.servicoyard.container.dto;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConteinerDetalheDTO {
    private Long identificador;
    private String identificacao;
    private String posicaoPatio;
    private TipoCargaConteiner tipoCarga;
    private BigDecimal pesoToneladas;
    private String restricoes;
    private StatusConteiner statusOperacional;
    private LocalDateTime ultimaAtualizacao;

    public ConteinerDetalheDTO(Long identificador, String identificacao, String posicaoPatio,
                               TipoCargaConteiner tipoCarga, BigDecimal pesoToneladas,
                               String restricoes, StatusConteiner statusOperacional,
                               LocalDateTime ultimaAtualizacao) {
        this.identificador = identificador;
        this.identificacao = identificacao;
        this.posicaoPatio = posicaoPatio;
        this.tipoCarga = tipoCarga;
        this.pesoToneladas = pesoToneladas;
        this.restricoes = restricoes;
        this.statusOperacional = statusOperacional;
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    public String getPosicaoPatio() {
        return posicaoPatio;
    }

    public TipoCargaConteiner getTipoCarga() {
        return tipoCarga;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public String getRestricoes() {
        return restricoes;
    }

    public StatusConteiner getStatusOperacional() {
        return statusOperacional;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }
}
