package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusOperacaoNavioConteiner;
import br.com.cloudport.serviconavio.atracacao.entidade.TipoOperacaoNavioConteiner;
import java.math.BigDecimal;

public class OperacaoNavioConteinerDTO {

    private final Long identificador;
    private final TipoOperacaoNavioConteiner tipoOperacao;
    private final String identificacaoConteiner;
    private final Integer bay;
    private final Integer fileira;
    private final Integer altura;
    private final BigDecimal pesoToneladas;
    private final StatusOperacaoNavioConteiner status;

    public OperacaoNavioConteinerDTO(Long identificador, TipoOperacaoNavioConteiner tipoOperacao,
                                     String identificacaoConteiner, Integer bay, Integer fileira,
                                     Integer altura, BigDecimal pesoToneladas,
                                     StatusOperacaoNavioConteiner status) {
        this.identificador = identificador;
        this.tipoOperacao = tipoOperacao;
        this.identificacaoConteiner = identificacaoConteiner;
        this.bay = bay;
        this.fileira = fileira;
        this.altura = altura;
        this.pesoToneladas = pesoToneladas;
        this.status = status;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public TipoOperacaoNavioConteiner getTipoOperacao() {
        return tipoOperacao;
    }

    public String getIdentificacaoConteiner() {
        return identificacaoConteiner;
    }

    public Integer getBay() {
        return bay;
    }

    public Integer getFileira() {
        return fileira;
    }

    public Integer getAltura() {
        return altura;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public StatusOperacaoNavioConteiner getStatus() {
        return status;
    }
}
