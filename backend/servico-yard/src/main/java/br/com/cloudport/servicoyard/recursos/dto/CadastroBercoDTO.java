package br.com.cloudport.servicoyard.recursos.dto;

import br.com.cloudport.servicoyard.recursos.entidade.StatusBerco;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CadastroBercoDTO {

    @NotBlank
    @Size(max = 30)
    private String codigo;

    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotNull
    @Min(1)
    private Integer comprimentoMetros;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal caladoMetros;

    @NotNull
    @Min(0)
    private Integer guinchesPermanentes;

    @NotNull
    @Min(1)
    private Integer capacidadeToneladasDia;

    @NotBlank
    @Size(max = 40)
    private String voltagem;

    private boolean aguaPotavel;
    private boolean energiaGenerica;
    private boolean iluminacaoNoturna;
    private boolean sistemaSeguranca;
    private boolean cobertura;
    private boolean compatContainer;
    private boolean compatBreakbulk;
    private boolean compatRoro;
    private boolean compatCargaGeral;
    private boolean compatReefer;
    private boolean compatPerigosa;
    private boolean compatGranel;

    @NotBlank
    @Size(max = 40)
    private String zonaPrimaria;

    @Size(max = 40)
    private String zonaSecundaria;

    @NotNull
    @Min(0)
    private Integer distanciaZonaMetros;

    @NotNull
    @Min(0)
    private Integer tempoTransporteMinutos;

    @NotBlank
    @Size(max = 80)
    private String diasOperacao;

    private LocalDate ultimaManutencao;
    private LocalDate proximaManutencao;

    @NotNull
    private StatusBerco status;

    @Size(max = 250)
    private String observacoes;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getComprimentoMetros() { return comprimentoMetros; }
    public void setComprimentoMetros(Integer comprimentoMetros) { this.comprimentoMetros = comprimentoMetros; }
    public BigDecimal getCaladoMetros() { return caladoMetros; }
    public void setCaladoMetros(BigDecimal caladoMetros) { this.caladoMetros = caladoMetros; }
    public Integer getGuinchesPermanentes() { return guinchesPermanentes; }
    public void setGuinchesPermanentes(Integer guinchesPermanentes) { this.guinchesPermanentes = guinchesPermanentes; }
    public Integer getCapacidadeToneladasDia() { return capacidadeToneladasDia; }
    public void setCapacidadeToneladasDia(Integer capacidadeToneladasDia) { this.capacidadeToneladasDia = capacidadeToneladasDia; }
    public String getVoltagem() { return voltagem; }
    public void setVoltagem(String voltagem) { this.voltagem = voltagem; }
    public boolean isAguaPotavel() { return aguaPotavel; }
    public void setAguaPotavel(boolean aguaPotavel) { this.aguaPotavel = aguaPotavel; }
    public boolean isEnergiaGenerica() { return energiaGenerica; }
    public void setEnergiaGenerica(boolean energiaGenerica) { this.energiaGenerica = energiaGenerica; }
    public boolean isIluminacaoNoturna() { return iluminacaoNoturna; }
    public void setIluminacaoNoturna(boolean iluminacaoNoturna) { this.iluminacaoNoturna = iluminacaoNoturna; }
    public boolean isSistemaSeguranca() { return sistemaSeguranca; }
    public void setSistemaSeguranca(boolean sistemaSeguranca) { this.sistemaSeguranca = sistemaSeguranca; }
    public boolean isCobertura() { return cobertura; }
    public void setCobertura(boolean cobertura) { this.cobertura = cobertura; }
    public boolean isCompatContainer() { return compatContainer; }
    public void setCompatContainer(boolean compatContainer) { this.compatContainer = compatContainer; }
    public boolean isCompatBreakbulk() { return compatBreakbulk; }
    public void setCompatBreakbulk(boolean compatBreakbulk) { this.compatBreakbulk = compatBreakbulk; }
    public boolean isCompatRoro() { return compatRoro; }
    public void setCompatRoro(boolean compatRoro) { this.compatRoro = compatRoro; }
    public boolean isCompatCargaGeral() { return compatCargaGeral; }
    public void setCompatCargaGeral(boolean compatCargaGeral) { this.compatCargaGeral = compatCargaGeral; }
    public boolean isCompatReefer() { return compatReefer; }
    public void setCompatReefer(boolean compatReefer) { this.compatReefer = compatReefer; }
    public boolean isCompatPerigosa() { return compatPerigosa; }
    public void setCompatPerigosa(boolean compatPerigosa) { this.compatPerigosa = compatPerigosa; }
    public boolean isCompatGranel() { return compatGranel; }
    public void setCompatGranel(boolean compatGranel) { this.compatGranel = compatGranel; }
    public String getZonaPrimaria() { return zonaPrimaria; }
    public void setZonaPrimaria(String zonaPrimaria) { this.zonaPrimaria = zonaPrimaria; }
    public String getZonaSecundaria() { return zonaSecundaria; }
    public void setZonaSecundaria(String zonaSecundaria) { this.zonaSecundaria = zonaSecundaria; }
    public Integer getDistanciaZonaMetros() { return distanciaZonaMetros; }
    public void setDistanciaZonaMetros(Integer distanciaZonaMetros) { this.distanciaZonaMetros = distanciaZonaMetros; }
    public Integer getTempoTransporteMinutos() { return tempoTransporteMinutos; }
    public void setTempoTransporteMinutos(Integer tempoTransporteMinutos) { this.tempoTransporteMinutos = tempoTransporteMinutos; }
    public String getDiasOperacao() { return diasOperacao; }
    public void setDiasOperacao(String diasOperacao) { this.diasOperacao = diasOperacao; }
    public LocalDate getUltimaManutencao() { return ultimaManutencao; }
    public void setUltimaManutencao(LocalDate ultimaManutencao) { this.ultimaManutencao = ultimaManutencao; }
    public LocalDate getProximaManutencao() { return proximaManutencao; }
    public void setProximaManutencao(LocalDate proximaManutencao) { this.proximaManutencao = proximaManutencao; }
    public StatusBerco getStatus() { return status; }
    public void setStatus(StatusBerco status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
