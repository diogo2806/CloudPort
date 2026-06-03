package br.com.cloudport.servicoyard.estivagembulk.dto;

public class PosicaoBobinaDto {

    private Long id;
    private Long bobinaId;
    private String codigoBobina;
    private double pesoKg;
    private Long poraoId;
    private int poraoNumero;
    private Long setorId;
    private String setorNome;
    private int camada;
    private double posicaoX;
    private double posicaoY;
    private double anguloInclinacao;
    private double espessuraDunnageMm;
    private String tipoLashing;
    private String alertaTanktop;

    public PosicaoBobinaDto() {
    }

    public PosicaoBobinaDto(Long id, Long bobinaId, String codigoBobina, double pesoKg, Long poraoId,
            int poraoNumero, Long setorId, String setorNome, int camada, double posicaoX, double posicaoY,
            double anguloInclinacao, double espessuraDunnageMm, String tipoLashing, String alertaTanktop) {
        this.id = id;
        this.bobinaId = bobinaId;
        this.codigoBobina = codigoBobina;
        this.pesoKg = pesoKg;
        this.poraoId = poraoId;
        this.poraoNumero = poraoNumero;
        this.setorId = setorId;
        this.setorNome = setorNome;
        this.camada = camada;
        this.posicaoX = posicaoX;
        this.posicaoY = posicaoY;
        this.anguloInclinacao = anguloInclinacao;
        this.espessuraDunnageMm = espessuraDunnageMm;
        this.tipoLashing = tipoLashing;
        this.alertaTanktop = alertaTanktop;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBobinaId() {
        return bobinaId;
    }

    public void setBobinaId(Long bobinaId) {
        this.bobinaId = bobinaId;
    }

    public String getCodigoBobina() {
        return codigoBobina;
    }

    public void setCodigoBobina(String codigoBobina) {
        this.codigoBobina = codigoBobina;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Long getPoraoId() {
        return poraoId;
    }

    public void setPoraoId(Long poraoId) {
        this.poraoId = poraoId;
    }

    public int getPoraoNumero() {
        return poraoNumero;
    }

    public void setPoraoNumero(int poraoNumero) {
        this.poraoNumero = poraoNumero;
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
    }

    public String getSetorNome() {
        return setorNome;
    }

    public void setSetorNome(String setorNome) {
        this.setorNome = setorNome;
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

    public double getAnguloInclinacao() {
        return anguloInclinacao;
    }

    public void setAnguloInclinacao(double anguloInclinacao) {
        this.anguloInclinacao = anguloInclinacao;
    }

    public double getEspessuraDunnageMm() {
        return espessuraDunnageMm;
    }

    public void setEspessuraDunnageMm(double espessuraDunnageMm) {
        this.espessuraDunnageMm = espessuraDunnageMm;
    }

    public String getTipoLashing() {
        return tipoLashing;
    }

    public void setTipoLashing(String tipoLashing) {
        this.tipoLashing = tipoLashing;
    }

    public String getAlertaTanktop() {
        return alertaTanktop;
    }

    public void setAlertaTanktop(String alertaTanktop) {
        this.alertaTanktop = alertaTanktop;
    }
}
