package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.estiva.entidade.TipoCargaConteiner;
import br.com.cloudport.serviconavio.estiva.entidade.TipoOperacaoEstiva;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class CriarAtribuicaoEstivaDTO {

    @NotNull(message = "Informe o tipo de operação (EMBARQUE ou DESCARGA).")
    private TipoOperacaoEstiva tipoOperacao;

    @NotBlank(message = "Informe o código do contêiner.")
    @Size(max = 20, message = "O código do contêiner deve ter no máximo 20 caracteres.")
    private String codigoConteiner;

    @NotNull(message = "Informe o tipo de carga do contêiner.")
    private TipoCargaConteiner tipoCarga;

    @PositiveOrZero(message = "O peso não pode ser negativo.")
    @DecimalMax(value = "99999.99", message = "O peso informado é inválido.")
    private BigDecimal pesoToneladas;

    @NotNull(message = "Informe a baia (bay) de destino.")
    @Min(value = 1, message = "A baia deve ser maior ou igual a 1.")
    private Integer baia;

    @NotNull(message = "Informe a fileira (row) de destino.")
    @Min(value = 1, message = "A fileira deve ser maior ou igual a 1.")
    private Integer fileira;

    @NotNull(message = "Informe a camada (tier) de destino.")
    @Min(value = 1, message = "A camada deve ser maior ou igual a 1.")
    private Integer camada;

    @Size(max = 40, message = "A posição de origem no pátio deve ter no máximo 40 caracteres.")
    private String posicaoPatioOrigem;

    @Size(max = 40, message = "A posição de destino no pátio deve ter no máximo 40 caracteres.")
    private String posicaoPatioDestino;

    @Min(value = 1, message = "A sequência de embarque deve ser maior ou igual a 1.")
    private Integer sequenciaEmbarque;

    public CriarAtribuicaoEstivaDTO() {
    }

    public TipoOperacaoEstiva getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacaoEstiva tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public TipoCargaConteiner getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(TipoCargaConteiner tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }

    public Integer getBaia() {
        return baia;
    }

    public void setBaia(Integer baia) {
        this.baia = baia;
    }

    public Integer getFileira() {
        return fileira;
    }

    public void setFileira(Integer fileira) {
        this.fileira = fileira;
    }

    public Integer getCamada() {
        return camada;
    }

    public void setCamada(Integer camada) {
        this.camada = camada;
    }

    public String getPosicaoPatioOrigem() {
        return posicaoPatioOrigem;
    }

    public void setPosicaoPatioOrigem(String posicaoPatioOrigem) {
        this.posicaoPatioOrigem = posicaoPatioOrigem;
    }

    public String getPosicaoPatioDestino() {
        return posicaoPatioDestino;
    }

    public void setPosicaoPatioDestino(String posicaoPatioDestino) {
        this.posicaoPatioDestino = posicaoPatioDestino;
    }

    public Integer getSequenciaEmbarque() {
        return sequenciaEmbarque;
    }

    public void setSequenciaEmbarque(Integer sequenciaEmbarque) {
        this.sequenciaEmbarque = sequenciaEmbarque;
    }
}
