package br.com.cloudport.servicoyard.recursos.dto;

import br.com.cloudport.servicoyard.recursos.entidade.StatusBerco;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BercoResumoDTO {

    private Long id;
    private String codigo;
    private String nome;
    private Integer comprimentoMetros;
    private BigDecimal caladoMetros;
    private Integer guinchesPermanentes;
    private Integer capacidadeToneladasDia;
    private String voltagem;
    private boolean aguaPotavel;
    private boolean energiaGenerica;
    private boolean iluminacaoNoturna;
    private boolean sistemaSeguranca;
    private boolean cobertura;
    private String zonaPrimaria;
    private String zonaSecundaria;
    private Integer distanciaZonaMetros;
    private Integer tempoTransporteMinutos;
    private String diasOperacao;
    private LocalDate ultimaManutencao;
    private LocalDate proximaManutencao;
    private StatusBerco status;
    private String observacoes;
    private int scoreAtual;
    private boolean recomendado;
    private String motivoRecomendacao;

    public BercoResumoDTO() {
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getComprimentoMetros() {
        return comprimentoMetros;
    }

    public void setComprimentoMetros(Integer comprimentoMetros) {
        this.comprimentoMetros = comprimentoMetros;
    }

    public BigDecimal getCaladoMetros() {
        return caladoMetros;
    }

    public void setCaladoMetros(BigDecimal caladoMetros) {
        this.caladoMetros = caladoMetros;
    }

    public Integer getGuinchesPermanentes() {
        return guinchesPermanentes;
    }

    public void setGuinchesPermanentes(Integer guinchesPermanentes) {
        this.guinchesPermanentes = guinchesPermanentes;
    }

    public Integer getCapacidadeToneladasDia() {
        return capacidadeToneladasDia;
    }

    public void setCapacidadeToneladasDia(Integer capacidadeToneladasDia) {
        this.capacidadeToneladasDia = capacidadeToneladasDia;
    }

    public String getVoltagem() {
        return voltagem;
    }

    public void setVoltagem(String voltagem) {
        this.voltagem = voltagem;
    }

    public boolean isAguaPotavel() {
        return aguaPotavel;
    }

    public void setAguaPotavel(boolean aguaPotavel) {
        this.aguaPotavel = aguaPotavel;
    }

    public boolean isEnergiaGenerica() {
        return energiaGenerica;
    }

    public void setEnergiaGenerica(boolean energiaGenerica) {
        this.energiaGenerica = energiaGenerica;
    }

    public boolean isIluminacaoNoturna() {
        return iluminacaoNoturna;
    }

    public void setIluminacaoNoturna(boolean iluminacaoNoturna) {
        this.iluminacaoNoturna = iluminacaoNoturna;
    }

    public boolean isSistemaSeguranca() {
        return sistemaSeguranca;
    }

    public void setSistemaSeguranca(boolean sistemaSeguranca) {
        this.sistemaSeguranca = sistemaSeguranca;
    }

    public boolean isCobertura() {
        return cobertura;
    }

    public void setCobertura(boolean cobertura) {
        this.cobertura = cobertura;
    }

    public String getZonaPrimaria() {
        return zonaPrimaria;
    }

    public void setZonaPrimaria(String zonaPrimaria) {
        this.zonaPrimaria = zonaPrimaria;
    }

    public String getZonaSecundaria() {
        return zonaSecundaria;
    }

    public void setZonaSecundaria(String zonaSecundaria) {
        this.zonaSecundaria = zonaSecundaria;
    }

    public Integer getDistanciaZonaMetros() {
        return distanciaZonaMetros;
    }

    public void setDistanciaZonaMetros(Integer distanciaZonaMetros) {
        this.distanciaZonaMetros = distanciaZonaMetros;
    }

    public Integer getTempoTransporteMinutos() {
        return tempoTransporteMinutos;
    }

    public void setTempoTransporteMinutos(Integer tempoTransporteMinutos) {
        this.tempoTransporteMinutos = tempoTransporteMinutos;
    }

    public String getDiasOperacao() {
        return diasOperacao;
    }

    public void setDiasOperacao(String diasOperacao) {
        this.diasOperacao = diasOperacao;
    }

    public LocalDate getUltimaManutencao() {
        return ultimaManutencao;
    }

    public void setUltimaManutencao(LocalDate ultimaManutencao) {
        this.ultimaManutencao = ultimaManutencao;
    }

    public LocalDate getProximaManutencao() {
        return proximaManutencao;
    }

    public void setProximaManutencao(LocalDate proximaManutencao) {
        this.proximaManutencao = proximaManutencao;
    }

    public StatusBerco getStatus() {
        return status;
    }

    public void setStatus(StatusBerco status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public int getScoreAtual() {
        return scoreAtual;
    }

    public void setScoreAtual(int scoreAtual) {
        this.scoreAtual = scoreAtual;
    }

    public boolean isRecomendado() {
        return recomendado;
    }

    public void setRecomendado(boolean recomendado) {
        this.recomendado = recomendado;
    }

    public String getMotivoRecomendacao() {
        return motivoRecomendacao;
    }

    public void setMotivoRecomendacao(String motivoRecomendacao) {
        this.motivoRecomendacao = motivoRecomendacao;
    }
}
