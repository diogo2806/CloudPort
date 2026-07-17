package br.com.cloudport.servicoyard.edi.modelo;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Posição EDIFACT bay/row/tier de um contêiner no navio.
 * Aceita BBRRTT (BAPLIE 1.x/2.x) e BBBRRTT (BAPLIE 3.1 D.13B).
 */
@Embeddable
public class PosicaoBay {

    @Column(name = "bay")
    private Integer bay;

    @Column(name = "row_bay")
    private Integer row;

    @Column(name = "tier")
    private Integer tier;

    public PosicaoBay() {
    }

    public PosicaoBay(Integer bay, Integer row, Integer tier) {
        this.bay = bay;
        this.row = row;
        this.tier = tier;
    }

    public static PosicaoBay deCodigoEdifact(String codigo) {
        if (codigo == null) {
            return new PosicaoBay(0, 0, 0);
        }

        String normalizado = codigo.trim();
        try {
            if (normalizado.matches("\\d{7,}")) {
                return new PosicaoBay(
                        Integer.parseInt(normalizado.substring(0, 3)),
                        Integer.parseInt(normalizado.substring(3, 5)),
                        Integer.parseInt(normalizado.substring(5, 7)));
            }
            if (normalizado.matches("\\d{6}")) {
                return new PosicaoBay(
                        Integer.parseInt(normalizado.substring(0, 2)),
                        Integer.parseInt(normalizado.substring(2, 4)),
                        Integer.parseInt(normalizado.substring(4, 6)));
            }
        } catch (NumberFormatException ignored) {
            // O retorno inválido é rejeitado pela validação obrigatória do parser.
        }
        return new PosicaoBay(0, 0, 0);
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
