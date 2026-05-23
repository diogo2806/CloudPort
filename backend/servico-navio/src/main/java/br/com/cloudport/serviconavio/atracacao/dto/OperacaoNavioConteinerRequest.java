package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.TipoOperacaoNavioConteiner;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class OperacaoNavioConteinerRequest {

    @NotNull(message = "Informe o tipo de operação (EMBARQUE ou DESCARGA).")
    private TipoOperacaoNavioConteiner tipoOperacao;

    @NotBlank(message = "Informe a identificação do contêiner.")
    @Size(max = 20, message = "A identificação do contêiner deve ter no máximo 20 caracteres.")
    private String identificacaoConteiner;

    private Integer bay;

    private Integer fileira;

    private Integer altura;

    @DecimalMin(value = "0.0", message = "O peso não pode ser negativo.")
    private BigDecimal pesoToneladas;

    public TipoOperacaoNavioConteiner getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacaoNavioConteiner tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getIdentificacaoConteiner() {
        return identificacaoConteiner;
    }

    public void setIdentificacaoConteiner(String identificacaoConteiner) {
        this.identificacaoConteiner = identificacaoConteiner;
    }

    public Integer getBay() {
        return bay;
    }

    public void setBay(Integer bay) {
        this.bay = bay;
    }

    public Integer getFileira() {
        return fileira;
    }

    public void setFileira(Integer fileira) {
        this.fileira = fileira;
    }

    public Integer getAltura() {
        return altura;
    }

    public void setAltura(Integer altura) {
        this.altura = altura;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }
}
