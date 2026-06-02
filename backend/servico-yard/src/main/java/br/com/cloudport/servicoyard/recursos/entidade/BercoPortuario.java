package br.com.cloudport.servicoyard.recursos.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "berco_portuario")
public class BercoPortuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "comprimento_metros", nullable = false)
    private Integer comprimentoMetros;

    @Column(name = "calado_metros", nullable = false, precision = 6, scale = 2)
    private BigDecimal caladoMetros;

    @Column(name = "guinches_permanentes", nullable = false)
    private Integer guinchesPermanentes;

    @Column(name = "capacidade_toneladas_dia", nullable = false)
    private Integer capacidadeToneladasDia;

    @Column(name = "voltagem", nullable = false, length = 40)
    private String voltagem;

    @Column(name = "agua_potavel", nullable = false)
    private boolean aguaPotavel;

    @Column(name = "energia_generica", nullable = false)
    private boolean energiaGenerica;

    @Column(name = "iluminacao_noturna", nullable = false)
    private boolean iluminacaoNoturna;

    @Column(name = "sistema_seguranca", nullable = false)
    private boolean sistemaSeguranca;

    @Column(name = "cobertura", nullable = false)
    private boolean cobertura;

    @Column(name = "compat_container", nullable = false)
    private boolean compatContainer;

    @Column(name = "compat_breakbulk", nullable = false)
    private boolean compatBreakbulk;

    @Column(name = "compat_roro", nullable = false)
    private boolean compatRoro;

    @Column(name = "compat_carga_geral", nullable = false)
    private boolean compatCargaGeral;

    @Column(name = "compat_reefer", nullable = false)
    private boolean compatReefer;

    @Column(name = "compat_perigosa", nullable = false)
    private boolean compatPerigosa;

    @Column(name = "compat_granel", nullable = false)
    private boolean compatGranel;

    @Column(name = "zona_primaria", nullable = false, length = 40)
    private String zonaPrimaria;

    @Column(name = "zona_secundaria", length = 40)
    private String zonaSecundaria;

    @Column(name = "distancia_zona_metros", nullable = false)
    private Integer distanciaZonaMetros;

    @Column(name = "tempo_transporte_minutos", nullable = false)
    private Integer tempoTransporteMinutos;

    @Column(name = "dias_operacao", nullable = false, length = 80)
    private String diasOperacao;

    @Column(name = "ultima_manutencao")
    private LocalDate ultimaManutencao;

    @Column(name = "proxima_manutencao")
    private LocalDate proximaManutencao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusBerco status;

    @Column(name = "observacoes", length = 250)
    private String observacoes;

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

    public boolean isCompatContainer() {
        return compatContainer;
    }

    public void setCompatContainer(boolean compatContainer) {
        this.compatContainer = compatContainer;
    }

    public boolean isCompatBreakbulk() {
        return compatBreakbulk;
    }

    public void setCompatBreakbulk(boolean compatBreakbulk) {
        this.compatBreakbulk = compatBreakbulk;
    }

    public boolean isCompatRoro() {
        return compatRoro;
    }

    public void setCompatRoro(boolean compatRoro) {
        this.compatRoro = compatRoro;
    }

    public boolean isCompatCargaGeral() {
        return compatCargaGeral;
    }

    public void setCompatCargaGeral(boolean compatCargaGeral) {
        this.compatCargaGeral = compatCargaGeral;
    }

    public boolean isCompatReefer() {
        return compatReefer;
    }

    public void setCompatReefer(boolean compatReefer) {
        this.compatReefer = compatReefer;
    }

    public boolean isCompatPerigosa() {
        return compatPerigosa;
    }

    public void setCompatPerigosa(boolean compatPerigosa) {
        this.compatPerigosa = compatPerigosa;
    }

    public boolean isCompatGranel() {
        return compatGranel;
    }

    public void setCompatGranel(boolean compatGranel) {
        this.compatGranel = compatGranel;
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
}
