package br.com.cloudport.servicoyard.vesselplanner.modelo;

import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import java.time.LocalDateTime;
import javax.persistence.Column;
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
@Table(name = "slot_navio")
public class SlotNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estivagem_plan_id")
    private EstivagemPlan estivagem;

    @Column(nullable = false)
    private int bay;

    @Column(name = "row_bay", nullable = false)
    private int rowBay;

    @Column(nullable = false)
    private int tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_slot", length = 20)
    private TipoSlotNavio tipoSlot;

    @Column(name = "max_peso_kg")
    private Double maxPesoKg;

    @Column(name = "pos_longitudinal_m")
    private Double posLongitudinalMetros;

    @Column(name = "pos_transversal_m")
    private Double posTransversalMetros;

    @Column(name = "pos_vertical_m")
    private Double posVerticalMetros;

    @Column(name = "codigo_container", length = 20)
    private String codigoContainer;

    @Column(name = "iso_code", length = 10)
    private String isoCode;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "peso_vgm_kg")
    private Double pesoVgmKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_carga", nullable = false, length = 20)
    private EstadoCargaContainer estadoCarga = EstadoCargaContainer.DESCONHECIDO;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Column(name = "classe_imo", length = 20)
    private String classeImo;

    @Column(name = "numero_onu", length = 20)
    private String numeroOnu;

    @Column(name = "grupo_segregacao", length = 50)
    private String grupoSegregacao;

    @Column(nullable = false)
    private boolean perigoso;

    @Column(nullable = false)
    private boolean reefer;

    @Column(name = "temperatura_requerida_c")
    private Double temperaturaRequeridaC;

    @Column(name = "temperatura_minima_c")
    private Double temperaturaMinimaC;

    @Column(name = "temperatura_maxima_c")
    private Double temperaturaMaximaC;

    @Column(nullable = false)
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

    @Column(name = "status_alertas", length = 20)
    private String statusAlertas;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EstivagemPlan getEstivagem() { return estivagem; }
    public void setEstivagem(EstivagemPlan estivagem) { this.estivagem = estivagem; }
    public int getBay() { return bay; }
    public void setBay(int bay) { this.bay = bay; }
    public int getRowBay() { return rowBay; }
    public void setRowBay(int rowBay) { this.rowBay = rowBay; }
    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }
    public TipoSlotNavio getTipoSlot() { return tipoSlot; }
    public void setTipoSlot(TipoSlotNavio tipoSlot) { this.tipoSlot = tipoSlot; }
    public Double getMaxPesoKg() { return maxPesoKg; }
    public void setMaxPesoKg(Double maxPesoKg) { this.maxPesoKg = maxPesoKg; }
    public Double getPosLongitudinalMetros() { return posLongitudinalMetros; }
    public void setPosLongitudinalMetros(Double posLongitudinalMetros) { this.posLongitudinalMetros = posLongitudinalMetros; }
    public Double getPosTransversalMetros() { return posTransversalMetros; }
    public void setPosTransversalMetros(Double posTransversalMetros) { this.posTransversalMetros = posTransversalMetros; }
    public Double getPosVerticalMetros() { return posVerticalMetros; }
    public void setPosVerticalMetros(Double posVerticalMetros) { this.posVerticalMetros = posVerticalMetros; }
    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }
    public Double getPesoVgmKg() { return pesoVgmKg; }
    public void setPesoVgmKg(Double pesoVgmKg) { this.pesoVgmKg = pesoVgmKg; }
    public EstadoCargaContainer getEstadoCarga() { return estadoCarga; }
    public void setEstadoCarga(EstadoCargaContainer estadoCarga) { this.estadoCarga = estadoCarga; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
    public String getClasseImo() { return classeImo; }
    public void setClasseImo(String classeImo) { this.classeImo = classeImo; }
    public String getNumeroOnu() { return numeroOnu; }
    public void setNumeroOnu(String numeroOnu) { this.numeroOnu = numeroOnu; }
    public String getGrupoSegregacao() { return grupoSegregacao; }
    public void setGrupoSegregacao(String grupoSegregacao) { this.grupoSegregacao = grupoSegregacao; }
    public boolean isPerigoso() { return perigoso; }
    public void setPerigoso(boolean perigoso) { this.perigoso = perigoso; }
    public boolean isReefer() { return reefer; }
    public void setReefer(boolean reefer) { this.reefer = reefer; }
    public Double getTemperaturaRequeridaC() { return temperaturaRequeridaC; }
    public void setTemperaturaRequeridaC(Double temperaturaRequeridaC) { this.temperaturaRequeridaC = temperaturaRequeridaC; }
    public Double getTemperaturaMinimaC() { return temperaturaMinimaC; }
    public void setTemperaturaMinimaC(Double temperaturaMinimaC) { this.temperaturaMinimaC = temperaturaMinimaC; }
    public Double getTemperaturaMaximaC() { return temperaturaMaximaC; }
    public void setTemperaturaMaximaC(Double temperaturaMaximaC) { this.temperaturaMaximaC = temperaturaMaximaC; }
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
    public String getStatusAlertas() { return statusAlertas; }
    public void setStatusAlertas(String statusAlertas) { this.statusAlertas = statusAlertas; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
