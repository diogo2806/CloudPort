package br.com.cloudport.servicoyard.edi.modelo;

import java.time.LocalDateTime;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
    @AttributeOverrides({
        @AttributeOverride(name = "bay",     column = @Column(name = "bay")),
        @AttributeOverride(name = "row_bay", column = @Column(name = "row_bay")),
        @AttributeOverride(name = "tier",    column = @Column(name = "tier"))
    })
    private PosicaoBay posicaoBay;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "referencia_bl", length = 30)
    private String referenciaBl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 20)
    private TipoOperacaoBayPlan tipoOperacao;

    @Column(name = "status_operacao", length = 20)
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
        if (criadoEm == null) criadoEm = atualizadoEm;
    }

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
