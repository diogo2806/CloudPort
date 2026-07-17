package br.com.cloudport.servicoyard.vesselplanner.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "slot_perfil_navio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_slot_perfil_navio_posicao",
                columnNames = {"perfil_geometria_navio_id", "bay", "row_bay", "tier"}))
public class SlotPerfilNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_geometria_navio_id", nullable = false)
    private PerfilGeometriaNavio perfil;

    @Column(nullable = false)
    private int bay;

    @Column(name = "row_bay", nullable = false)
    private int rowBay;

    @Column(nullable = false)
    private int tier;

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

    @Column(name = "max_peso_kg", nullable = false)
    private Double maxPesoKg;

    @Column(name = "max_peso_pilha_kg", nullable = false)
    private Double maxPesoPilhaKg;

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

    public PerfilGeometriaNavio getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilGeometriaNavio perfil) {
        this.perfil = perfil;
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
}
