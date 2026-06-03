package br.com.cloudport.servicoyard.edi.modelo;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Posição EDIFACT bay/row/tier de um contêiner no navio.
 * Codificação padrão: string de 6 dígitos BBRRTT
 * BB = bay (01-99), RR = row (00=centerline, ímpar=BB, par=BE), TT = tier (≥02 acima, ≥82 abaixo)
 */
@Embeddable
public class PosicaoBay {

    @Column(name = "bay")
    private Integer bay;

    @Column(name = "row_bay")
    private Integer row;

    @Column(name = "tier")
    private Integer tier;

    public PosicaoBay() {}

    public PosicaoBay(Integer bay, Integer row, Integer tier) {
        this.bay = bay;
        this.row = row;
        this.tier = tier;
    }

    /**
     * Cria uma PosicaoBay a partir da string de 6 dígitos BBRRTT do padrão EDIFACT.
     */
    public static PosicaoBay deCodigoEdifact(String codigo) {
        if (codigo == null || codigo.length() < 6) {
            return new PosicaoBay(0, 0, 0);
        }
        try {
            int bay = Integer.parseInt(codigo.substring(0, 2));
            int row = Integer.parseInt(codigo.substring(2, 4));
            int tier = Integer.parseInt(codigo.substring(4, 6));
            return new PosicaoBay(bay, row, tier);
        } catch (NumberFormatException e) {
            return new PosicaoBay(0, 0, 0);
        }
    }

    public String toCodigoEdifact() {
        return String.format("%02d%02d%02d", bay, row, tier);
    }

    public boolean isAbaisoDoConves() {
        return tier != null && tier >= 82;
    }

    public Integer getBay() { return bay; }
    public Integer getRow() { return row; }
    public Integer getTier() { return tier; }
    public void setBay(Integer bay) { this.bay = bay; }
    public void setRow(Integer row) { this.row = row; }
    public void setTier(Integer tier) { this.tier = tier; }
}
