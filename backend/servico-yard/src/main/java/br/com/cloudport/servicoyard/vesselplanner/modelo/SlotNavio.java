package br.com.cloudport.servicoyard.vesselplanner.modelo;

import javax.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "codigo_container", length = 20)
    private String codigoContainer;

    @Column(name = "iso_code", length = 10)
    private String isoCode;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Column(name = "classe_imo", length = 10)
    private String classeImo;

    @Column
    private boolean reefer;

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
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EstivagemPlan getEstivagem() {
        return estivagem;
    }

    public void setEstivagem(EstivagemPlan estivagem) {
        this.estivagem = estivagem;
    }

    public int getBay() {
        return bay;
    }

    public void setBay(int bay) {
        this.bay = bay;
    }

    public int getRowBay() {
        return rowBay;
    }

    public void setRowBay(int rowBay) {
        this.rowBay = rowBay;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public TipoSlotNavio getTipoSlot() {
        return tipoSlot;
    }

    public void setTipoSlot(TipoSlotNavio tipoSlot) {
        this.tipoSlot = tipoSlot;
    }

    public Double getMaxPesoKg() {
        return maxPesoKg;
    }

    public void setMaxPesoKg(Double maxPesoKg) {
        this.maxPesoKg = maxPesoKg;
    }

    public String getCodigoContainer() {
        return codigoContainer;
    }

    public void setCodigoContainer(String codigoContainer) {
        this.codigoContainer = codigoContainer;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public String getPortoCarga() {
        return portoCarga;
    }

    public void setPortoCarga(String portoCarga) {
        this.portoCarga = portoCarga;
    }

    public String getPortoDescarga() {
        return portoDescarga;
    }

    public void setPortoDescarga(String portoDescarga) {
        this.portoDescarga = portoDescarga;
    }

    public String getClasseImo() {
        return classeImo;
    }

    public void setClasseImo(String classeImo) {
        this.classeImo = classeImo;
    }

    public boolean isReefer() {
        return reefer;
    }

    public void setReefer(boolean reefer) {
        this.reefer = reefer;
    }

    public String getStatusAlertas() {
        return statusAlertas;
    }

    public void setStatusAlertas(String statusAlertas) {
        this.statusAlertas = statusAlertas;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
