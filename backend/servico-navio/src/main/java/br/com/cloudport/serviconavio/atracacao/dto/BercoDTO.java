package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusBerco;
import java.math.BigDecimal;

public class BercoDTO {

    private final Long identificador;
    private final String nome;
    private final BigDecimal comprimentoMetros;
    private final BigDecimal caladoMaximoMetros;
    private final StatusBerco status;

    public BercoDTO(Long identificador, String nome, BigDecimal comprimentoMetros,
                    BigDecimal caladoMaximoMetros, StatusBerco status) {
        this.identificador = identificador;
        this.nome = nome;
        this.comprimentoMetros = comprimentoMetros;
        this.caladoMaximoMetros = caladoMaximoMetros;
        this.status = status;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getComprimentoMetros() {
        return comprimentoMetros;
    }

    public BigDecimal getCaladoMaximoMetros() {
        return caladoMaximoMetros;
    }

    public StatusBerco getStatus() {
        return status;
    }
}
