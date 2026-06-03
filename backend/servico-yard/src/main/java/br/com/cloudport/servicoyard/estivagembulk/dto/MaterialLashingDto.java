package br.com.cloudport.servicoyard.estivagembulk.dto;

public class MaterialLashingDto {

    private String tipo;
    private int quantidade;
    private double comprimentoM;
    private double pesoUnitarioKg;
    private String descricao;

    public MaterialLashingDto() {
    }

    public MaterialLashingDto(String tipo, int quantidade, double comprimentoM, double pesoUnitarioKg,
            String descricao) {
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.comprimentoM = comprimentoM;
        this.pesoUnitarioKg = pesoUnitarioKg;
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getComprimentoM() {
        return comprimentoM;
    }

    public void setComprimentoM(double comprimentoM) {
        this.comprimentoM = comprimentoM;
    }

    public double getPesoUnitarioKg() {
        return pesoUnitarioKg;
    }

    public void setPesoUnitarioKg(double pesoUnitarioKg) {
        this.pesoUnitarioKg = pesoUnitarioKg;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
