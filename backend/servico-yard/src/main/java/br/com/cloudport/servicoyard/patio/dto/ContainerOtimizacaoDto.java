package br.com.cloudport.servicoyard.patio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ContainerOtimizacaoDto {

    @NotNull
    private Long id;

    @NotBlank
    private String codigo;

    @NotNull
    private LocalDateTime etaPartida;

    private BigDecimal pesoToneladas;

    private String tipoCarga;

    private String destino;

    private String restricoes;

    public ContainerOtimizacaoDto() {
    }

    public ContainerOtimizacaoDto(Long id, String codigo, LocalDateTime etaPartida) {
        this.id = id;
        this.codigo = codigo;
        this.etaPartida = etaPartida;
    }

    public ContainerOtimizacaoDto(Long id, String codigo, LocalDateTime etaPartida,
                                  BigDecimal pesoToneladas, String tipoCarga, String destino) {
        this.id = id;
        this.codigo = codigo;
        this.etaPartida = etaPartida;
        this.pesoToneladas = pesoToneladas;
        this.tipoCarga = tipoCarga;
        this.destino = destino;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getEtaPartida() {
        return etaPartida;
    }

    public void setEtaPartida(LocalDateTime etaPartida) {
        this.etaPartida = etaPartida;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getRestricoes() {
        return restricoes;
    }

    public void setRestricoes(String restricoes) {
        this.restricoes = restricoes;
    }
}
