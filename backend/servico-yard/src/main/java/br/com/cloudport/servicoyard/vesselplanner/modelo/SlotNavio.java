package br.com.cloudport.servicoyard.vesselplanner.modelo;

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

    @Column(name = "codigo_hatch_cover", length = 40)
    private String codigoHatchCover;

    @Column(name = "sobre_hatch_cover", nullable = false)
    private boolean sobreHatchCover;

    @Column(nullable = false)
    private boolean restrito;

    @Column(name = "motivo_restricao", length = 255)
    private String motivoRestricao;

    @Column(name = "tomada_reefer", nullable = false)
    private boolean tomadaReefer;

    @Column(name = "aceita_20_pes", nullable = false)
    private boolean aceita20Pes;

    @Column(name = "aceita_40_pes", nullable = false)
    private boolean aceita40Pes;

    @Column(name = "aceita_45_pes", nullable = false)
    private boolean aceita45Pes;

    @Column(name = "max_peso_kg")
    private Double maxPesoKg;

    @Column(name = "max_peso_pilha_kg")
    private Double maxPesoPilhaKg;

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

    public boolean aceitaComprimentoPes(int comprimentoPes) {
        if (comprimentoPes == 20) {
            return aceita20Pes;
        }
        if (comprimentoPes == 40) {
            return aceita40Pes;
        }
        if (comprimentoPes == 45) {
            return aceita45Pes;
        }
        return false;
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

    public String getCodigoHatchCover() {
        return codigoHatchCover;
    }

    public void setCodigoHatchCover(String codigoHatchCover) {
        this.codigoHatchCover = codigoHatchCover;
    }

    public boolean isSobreHatchCover() {
        return sobreHatchCover;
    }

    public void setSobreHatchCover(boolean sobreHatchCover) {
        this.sobreHatchCover = sobreHatchCover;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public void setRestrito(boolean restrito) {
        this.restrito = restrito;
    }

    public String getMotivoRestricao() {
        return motivoRestricao;
    }

    public void setMotivoRestricao(String motivoRestricao) {
        this.motivoRestricao = motivoRestricao;
    }

    public boolean isTomadaReefer() {
        return tomadaReefer;
    }

    public void setTomadaReefer(boolean tomadaReefer) {
        this.tomadaReefer = tomadaReefer;
    }

    public boolean isAceita20Pes() {
        return aceita20Pes;
    }

    public void setAceita20Pes(boolean aceita20Pes) {
        this.aceita20Pes = aceita20Pes;
    }

    public boolean isAceita40Pes() {
        return aceita40Pes;
    }

    public void setAceita40Pes(boolean aceita40Pes) {
        this.aceita40Pes = aceita40Pes;
    }

    public boolean isAceita45Pes() {
        return aceita45Pes;
    }

    public void setAceita45Pes(boolean aceita45Pes) {
        this.aceita45Pes = aceita45Pes;
    }

    public Double getMaxPesoKg() {
        return maxPesoKg;
    }

    public void setMaxPesoKg(Double maxPesoKg) {
        this.maxPesoKg = maxPesoKg;
    }

    public Double getMaxPesoPilhaKg() {
        return maxPesoPilhaKg;
    }

    public void setMaxPesoPilhaKg(Double maxPesoPilhaKg) {
        this.maxPesoPilhaKg = maxPesoPilhaKg;
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
