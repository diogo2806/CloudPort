package br.com.cloudport.servicoyard.estivagembulk.dto;

import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;

public class PosicionarBobinaRequisicaoDto {

    private Long bobinaId;
    private Long poraoId;
    private Long setorId;
    private int camada;
    private double posicaoX;
    private double posicaoY;
    private double espessuraDunnageMm;
    private TipoLashing tipoLashing;

    public PosicionarBobinaRequisicaoDto() {
    }

    public PosicionarBobinaRequisicaoDto(Long bobinaId, Long poraoId, Long setorId, int camada,
            double posicaoX, double posicaoY, double espessuraDunnageMm, TipoLashing tipoLashing) {
        this.bobinaId = bobinaId;
        this.poraoId = poraoId;
        this.setorId = setorId;
        this.camada = camada;
        this.posicaoX = posicaoX;
        this.posicaoY = posicaoY;
        this.espessuraDunnageMm = espessuraDunnageMm;
        this.tipoLashing = tipoLashing;
    }

    public Long getBobinaId() {
        return bobinaId;
    }

    public void setBobinaId(Long bobinaId) {
        this.bobinaId = bobinaId;
    }

    public Long getPoraoId() {
        return poraoId;
    }

    public void setPoraoId(Long poraoId) {
        this.poraoId = poraoId;
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
    }

    public int getCamada() {
        return camada;
    }

    public void setCamada(int camada) {
        this.camada = camada;
    }

    public double getPosicaoX() {
        return posicaoX;
    }

    public void setPosicaoX(double posicaoX) {
        this.posicaoX = posicaoX;
    }

    public double getPosicaoY() {
        return posicaoY;
    }

    public void setPosicaoY(double posicaoY) {
        this.posicaoY = posicaoY;
    }

    public double getEspessuraDunnageMm() {
        return espessuraDunnageMm;
    }

    public void setEspessuraDunnageMm(double espessuraDunnageMm) {
        this.espessuraDunnageMm = espessuraDunnageMm;
    }

    public TipoLashing getTipoLashing() {
        return tipoLashing;
    }

    public void setTipoLashing(TipoLashing tipoLashing) {
        this.tipoLashing = tipoLashing;
    }
}
