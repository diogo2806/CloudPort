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

    private LocalDateTime etaChegada;

    private LocalDateTime etaPartida;

    private BigDecimal pesoToneladas;

    private String tipoCarga;

    private String destino;

    private String restricoes;

    private String categoria;

    private String armador;

    private String visitaSaida;

    private Integer comprimentoPes;

    private String tipoEquipamento;

    private String estadoCarga;

    private Boolean refrigerado;

    private Boolean perigoso;

    private String classeImo;

    private String numeroOnu;

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

    public LocalDateTime getEtaChegada() {
        return etaChegada;
    }

    public void setEtaChegada(LocalDateTime etaChegada) {
        this.etaChegada = etaChegada;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getArmador() {
        return armador;
    }

    public void setArmador(String armador) {
        this.armador = armador;
    }

    public String getVisitaSaida() {
        return visitaSaida;
    }

    public void setVisitaSaida(String visitaSaida) {
        this.visitaSaida = visitaSaida;
    }

    public Integer getComprimentoPes() {
        return comprimentoPes;
    }

    public void setComprimentoPes(Integer comprimentoPes) {
        this.comprimentoPes = comprimentoPes;
    }

    public String getTipoEquipamento() {
        return tipoEquipamento;
    }

    public void setTipoEquipamento(String tipoEquipamento) {
        this.tipoEquipamento = tipoEquipamento;
    }

    public String getEstadoCarga() {
        return estadoCarga;
    }

    public void setEstadoCarga(String estadoCarga) {
        this.estadoCarga = estadoCarga;
    }

    public Boolean getRefrigerado() {
        return refrigerado;
    }

    public void setRefrigerado(Boolean refrigerado) {
        this.refrigerado = refrigerado;
    }

    public Boolean getPerigoso() {
        return perigoso;
    }

    public void setPerigoso(Boolean perigoso) {
        this.perigoso = perigoso;
    }

    public String getClasseImo() {
        return classeImo;
    }

    public void setClasseImo(String classeImo) {
        this.classeImo = classeImo;
    }

    public String getNumeroOnu() {
        return numeroOnu;
    }

    public void setNumeroOnu(String numeroOnu) {
        this.numeroOnu = numeroOnu;
    }
}
