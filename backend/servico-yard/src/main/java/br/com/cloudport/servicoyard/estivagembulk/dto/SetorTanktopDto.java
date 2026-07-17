package br.com.cloudport.servicoyard.estivagembulk.dto;

public class SetorTanktopDto {

    private Long id;
    private String nome;
    private Double capacidadeTM2;
    private Double areaM2;
    private Double posLongInicio;
    private Double posLongFim;
    private Double posTransInicio;
    private Double posTransFim;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getCapacidadeTM2() {
        return capacidadeTM2;
    }

    public void setCapacidadeTM2(Double capacidadeTM2) {
        this.capacidadeTM2 = capacidadeTM2;
    }

    public Double getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(Double areaM2) {
        this.areaM2 = areaM2;
    }

    public Double getPosLongInicio() {
        return posLongInicio;
    }

    public void setPosLongInicio(Double posLongInicio) {
        this.posLongInicio = posLongInicio;
    }

    public Double getPosLongFim() {
        return posLongFim;
    }

    public void setPosLongFim(Double posLongFim) {
        this.posLongFim = posLongFim;
    }

    public Double getPosTransInicio() {
        return posTransInicio;
    }

    public void setPosTransInicio(Double posTransInicio) {
        this.posTransInicio = posTransInicio;
    }

    public Double getPosTransFim() {
        return posTransFim;
    }

    public void setPosTransFim(Double posTransFim) {
        this.posTransFim = posTransFim;
    }
}
