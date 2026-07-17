package br.com.cloudport.servicoyard.edi.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "bay_plan_container")
public class BayPlanContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bay_plan_id", nullable = false)
    private BayPlan bayPlan;

    @Column(name = "codigo_container", nullable = false, length = 20)
    private String codigoContainer;

    @Column(name = "iso_code", length = 10)
    private String isoCode;

    @Embedded
    private PosicaoBay posicaoBay;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "unidade_peso_original", length = 10)
    private String unidadePesoOriginal;

    @Column(name = "peso_vgm_kg")
    private Double pesoVgmKg;

    @Column(name = "unidade_vgm_original", length = 10)
    private String unidadeVgmOriginal;

    @Column(name = "origem_vgm", length = 30)
    private String origemVgm;

    @Column(name = "status_vgm", length = 30)
    private String statusVgm;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_carga", nullable = false, length = 20)
    private EstadoCargaContainer estadoCarga = EstadoCargaContainer.DESCONHECIDO;

    @Column(name = "reefer", nullable = false)
    private boolean reefer;

    @Column(name = "temperatura_requerida_c")
    private Double temperaturaRequeridaC;

    @Column(name = "temperatura_minima_c")
    private Double temperaturaMinimaC;

    @Column(name = "temperatura_maxima_c")
    private Double temperaturaMaximaC;

    @Column(name = "perigoso", nullable = false)
    private boolean perigoso;

    @Column(name = "classe_imo", length = 20)
    private String classeImo;

    @Column(name = "numero_onu", length = 20)
    private String numeroOnu;

    @Column(name = "grupo_embalagem", length = 20)
    private String grupoEmbalagem;

    @Column(name = "grupo_segregacao", length = 50)
    private String grupoSegregacao;

    @Column(name = "codigo_emergencia", length = 100)
    private String codigoEmergencia;

    @Column(name = "oog", nullable = false)
    private boolean oog;

    @Column(name = "excesso_frontal_cm")
    private Double excessoFrontalCm;

    @Column(name = "excesso_traseiro_cm")
    private Double excessoTraseiroCm;

    @Column(name = "excesso_esquerdo_cm")
    private Double excessoEsquerdoCm;

    @Column(name = "excesso_direito_cm")
    private Double excessoDireitoCm;

    @Column(name = "excesso_altura_cm")
    private Double excessoAlturaCm;

    @Column(name = "instrucao_manuseio", length = 500)
    private String instrucaoManuseio;

    @Column(name = "segmentos_originais", columnDefinition = "TEXT")
    private String segmentosOriginais;

    @Column(name = "referencia_bl", length = 30)
    private String referenciaBl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 20)
    private TipoOperacaoBayPlan tipoOperacao;

    @Column(name = "status_operacao", length = 30)
    private String statusOperacao;

    @Column(name = "horario_operacao")
    private LocalDateTime horarioOperacao;

    @Column(name = "linha_yard")
    private Integer linhaYard;

    @Column(name = "coluna_yard")
    private Integer colunaYard;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
        if (estadoCarga == null) {
            estadoCarga = EstadoCargaContainer.DESCONHECIDO;
        }
    }

    public Double getPesoOperacionalKg() { return pesoVgmKg != null ? pesoVgmKg : pesoKg; }
    public Long getId() { return id; }
    public BayPlan getBayPlan() { return bayPlan; }
    public void setBayPlan(BayPlan bayPlan) { this.bayPlan = bayPlan; }
    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
    public PosicaoBay getPosicaoBay() { return posicaoBay; }
    public void setPosicaoBay(PosicaoBay posicaoBay) { this.posicaoBay = posicaoBay; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }
    public String getUnidadePesoOriginal() { return unidadePesoOriginal; }
    public void setUnidadePesoOriginal(String unidadePesoOriginal) { this.unidadePesoOriginal = unidadePesoOriginal; }
    public Double getPesoVgmKg() { return pesoVgmKg; }
    public void setPesoVgmKg(Double pesoVgmKg) { this.pesoVgmKg = pesoVgmKg; }
    public String getUnidadeVgmOriginal() { return unidadeVgmOriginal; }
    public void setUnidadeVgmOriginal(String unidadeVgmOriginal) { this.unidadeVgmOriginal = unidadeVgmOriginal; }
    public String getOrigemVgm() { return origemVgm; }
    public void setOrigemVgm(String origemVgm) { this.origemVgm = origemVgm; }
    public String getStatusVgm() { return statusVgm; }
    public void setStatusVgm(String statusVgm) { this.statusVgm = statusVgm; }
    public EstadoCargaContainer getEstadoCarga() { return estadoCarga; }
    public void setEstadoCarga(EstadoCargaContainer estadoCarga) { this.estadoCarga = estadoCarga; }
    public boolean isReefer() { return reefer; }
    public void setReefer(boolean reefer) { this.reefer = reefer; }
    public Double getTemperaturaRequeridaC() { return temperaturaRequeridaC; }
    public void setTemperaturaRequeridaC(Double temperaturaRequeridaC) { this.temperaturaRequeridaC = temperaturaRequeridaC; }
    public Double getTemperaturaMinimaC() { return temperaturaMinimaC; }
    public void setTemperaturaMinimaC(Double temperaturaMinimaC) { this.temperaturaMinimaC = temperaturaMinimaC; }
    public Double getTemperaturaMaximaC() { return temperaturaMaximaC; }
    public void setTemperaturaMaximaC(Double temperaturaMaximaC) { this.temperaturaMaximaC = temperaturaMaximaC; }
    public boolean isPerigoso() { return perigoso; }
    public void setPerigoso(boolean perigoso) { this.perigoso = perigoso; }
    public String getClasseImo() { return classeImo; }
    public void setClasseImo(String classeImo) { this.classeImo = classeImo; }
    public String getNumeroOnu() { return numeroOnu; }
    public void setNumeroOnu(String numeroOnu) { this.numeroOnu = numeroOnu; }
    public String getGrupoEmbalagem() { return grupoEmbalagem; }
    public void setGrupoEmbalagem(String grupoEmbalagem) { this.grupoEmbalagem = grupoEmbalagem; }
    public String getGrupoSegregacao() { return grupoSegregacao; }
    public void setGrupoSegregacao(String grupoSegregacao) { this.grupoSegregacao = grupoSegregacao; }
    public String getCodigoEmergencia() { return codigoEmergencia; }
    public void setCodigoEmergencia(String codigoEmergencia) { this.codigoEmergencia = codigoEmergencia; }
    public boolean isOog() { return oog; }
    public void setOog(boolean oog) { this.oog = oog; }
    public Double getExcessoFrontalCm() { return excessoFrontalCm; }
    public void setExcessoFrontalCm(Double excessoFrontalCm) { this.excessoFrontalCm = excessoFrontalCm; }
    public Double getExcessoTraseiroCm() { return excessoTraseiroCm; }
    public void setExcessoTraseiroCm(Double excessoTraseiroCm) { this.excessoTraseiroCm = excessoTraseiroCm; }
    public Double getExcessoEsquerdoCm() { return excessoEsquerdoCm; }
    public void setExcessoEsquerdoCm(Double excessoEsquerdoCm) { this.excessoEsquerdoCm = excessoEsquerdoCm; }
    public Double getExcessoDireitoCm() { return excessoDireitoCm; }
    public void setExcessoDireitoCm(Double excessoDireitoCm) { this.excessoDireitoCm = excessoDireitoCm; }
    public Double getExcessoAlturaCm() { return excessoAlturaCm; }
    public void setExcessoAlturaCm(Double excessoAlturaCm) { this.excessoAlturaCm = excessoAlturaCm; }
    public String getInstrucaoManuseio() { return instrucaoManuseio; }
    public void setInstrucaoManuseio(String instrucaoManuseio) { this.instrucaoManuseio = instrucaoManuseio; }
    public String getSegmentosOriginais() { return segmentosOriginais; }
    public void setSegmentosOriginais(String segmentosOriginais) { this.segmentosOriginais = segmentosOriginais; }
    public String getReferenciaBl() { return referenciaBl; }
    public void setReferenciaBl(String referenciaBl) { this.referenciaBl = referenciaBl; }
    public TipoOperacaoBayPlan getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoBayPlan tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public String getStatusOperacao() { return statusOperacao; }
    public void setStatusOperacao(String statusOperacao) { this.statusOperacao = statusOperacao; }
    public LocalDateTime getHorarioOperacao() { return horarioOperacao; }
    public void setHorarioOperacao(LocalDateTime horarioOperacao) { this.horarioOperacao = horarioOperacao; }
    public Integer getLinhaYard() { return linhaYard; }
    public void setLinhaYard(Integer linhaYard) { this.linhaYard = linhaYard; }
    public Integer getColunaYard() { return colunaYard; }
    public void setColunaYard(Integer colunaYard) { this.colunaYard = colunaYard; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
