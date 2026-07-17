package br.com.cloudport.servicoyard.estivagembulk.dto;

public class BobinaManifestoDto {

    private Long id;
    private String codigo;
    private Double pesoKg;
    private Double diametroExternoMm;
    private Double diametroInternoMm;
    private Double larguraMm;
    private String grauAco;
    private String portoDescarga;
    private boolean posicionada;

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

    public boolean isPosicionada() {
        return posicionada;
    }

    public void setPosicionada(boolean posicionada) {
        this.posicionada = posicionada;
    }
}
