package br.com.cloudport.servicoyard.estivagembulk.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "bobina_manifesto")
public class BobinaManifesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id")
    private PlanoEstivaBulk plano;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "diametro_externo_mm")
    private Double diametroExternoMm;

    @Column(name = "diametro_interno_mm")
    private Double diametroInternoMm;

    @Column(name = "largura_mm")
    private Double larguraMm;

    @Column(name = "grau_aco", length = 20)
    private String grauAco;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanoEstivaBulk getPlano() {
        return plano;
    }

    public void setPlano(PlanoEstivaBulk plano) {
        this.plano = plano;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Double getDiametroExternoMm() {
        return diametroExternoMm;
    }

    public void setDiametroExternoMm(Double diametroExternoMm) {
        this.diametroExternoMm = diametroExternoMm;
    }

    public Double getDiametroInternoMm() {
        return diametroInternoMm;
    }

    public void setDiametroInternoMm(Double diametroInternoMm) {
        this.diametroInternoMm = diametroInternoMm;
    }

    public Double getLarguraMm() {
        return larguraMm;
    }

    public void setLarguraMm(Double larguraMm) {
        this.larguraMm = larguraMm;
    }

    public String getGrauAco() {
        return grauAco;
    }

    public void setGrauAco(String grauAco) {
        this.grauAco = grauAco;
    }

    public String getPortoDescarga() {
        return portoDescarga;
    }

    public void setPortoDescarga(String portoDescarga) {
        this.portoDescarga = portoDescarga;
    }
}
