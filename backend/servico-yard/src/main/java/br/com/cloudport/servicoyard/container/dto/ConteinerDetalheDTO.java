package br.com.cloudport.servicoyard.container.dto;

import br.com.cloudport.servicoyard.container.entidade.StatusOperacionalConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ConteinerDetalheDTO {
    private Long identificador;
    private String identificacao;
    private String posicaoPatio;
    private TipoCargaConteiner tipoCarga;
    private BigDecimal pesoToneladas;
    private String restricoes;
    private StatusOperacionalConteiner statusOperacional;
    private OffsetDateTime ultimaAtualizacao;

    public ConteinerDetalheDTO(Long identificador, String identificacao, String posicaoPatio,
                               TipoCargaConteiner tipoCarga, BigDecimal pesoToneladas,
                               String restricoes, StatusOperacionalConteiner statusOperacional,
                               OffsetDateTime ultimaAtualizacao) {
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

    public StatusOperacionalConteiner getStatusOperacional() {
        return statusOperacional;
    }

    public OffsetDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }
}
