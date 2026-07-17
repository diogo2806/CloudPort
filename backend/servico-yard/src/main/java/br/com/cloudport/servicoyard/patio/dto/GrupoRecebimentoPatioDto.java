package br.com.cloudport.servicoyard.patio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GrupoRecebimentoPatioDto {

    private String chaveAgrupamento;
    private String nome;
    private Integer prioridade;
    private String categoria;
    private String armador;
    private String visitaSaida;
    private String destino;
    private Integer comprimentoPes;
    private String tipoEquipamento;
    private String estadoCarga;
    private Boolean refrigerado;
    private Boolean perigoso;
    private String classeImo;
    private String faixaPeso;
    private LocalDateTime inicioJanelaRecebimento;
    private LocalDateTime fimJanelaRecebimento;
    private Integer quantidadeConteineres;
    private Integer teus;
    private BigDecimal pesoTotalToneladas;
    private List<String> alertas = new ArrayList<>();
    private List<ContainerOtimizacaoDto> conteineres = new ArrayList<>();

    public String getChaveAgrupamento() {
        return chaveAgrupamento;
    }

    public void setChaveAgrupamento(String chaveAgrupamento) {
        this.chaveAgrupamento = chaveAgrupamento;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Integer prioridade) {
        this.prioridade = prioridade;
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

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
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

    public String getFaixaPeso() {
        return faixaPeso;
    }

    public void setFaixaPeso(String faixaPeso) {
        this.faixaPeso = faixaPeso;
    }

    public LocalDateTime getInicioJanelaRecebimento() {
        return inicioJanelaRecebimento;
    }

    public void setInicioJanelaRecebimento(LocalDateTime inicioJanelaRecebimento) {
        this.inicioJanelaRecebimento = inicioJanelaRecebimento;
    }

    public LocalDateTime getFimJanelaRecebimento() {
        return fimJanelaRecebimento;
    }

    public void setFimJanelaRecebimento(LocalDateTime fimJanelaRecebimento) {
        this.fimJanelaRecebimento = fimJanelaRecebimento;
    }

    public Integer getQuantidadeConteineres() {
        return quantidadeConteineres;
    }

    public void setQuantidadeConteineres(Integer quantidadeConteineres) {
        this.quantidadeConteineres = quantidadeConteineres;
    }

    public Integer getTeus() {
        return teus;
    }

    public void setTeus(Integer teus) {
        this.teus = teus;
    }

    public BigDecimal getPesoTotalToneladas() {
        return pesoTotalToneladas;
    }

    public void setPesoTotalToneladas(BigDecimal pesoTotalToneladas) {
        this.pesoTotalToneladas = pesoTotalToneladas;
    }

    public List<String> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<String> alertas) {
        this.alertas = alertas == null ? new ArrayList<>() : new ArrayList<>(alertas);
    }

    public List<ContainerOtimizacaoDto> getConteineres() {
        return conteineres;
    }

    public void setConteineres(List<ContainerOtimizacaoDto> conteineres) {
        this.conteineres = conteineres == null ? new ArrayList<>() : new ArrayList<>(conteineres);
    }
}
