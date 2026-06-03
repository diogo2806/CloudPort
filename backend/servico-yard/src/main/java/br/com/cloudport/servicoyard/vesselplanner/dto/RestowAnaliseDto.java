package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.List;

public class RestowAnaliseDto {

    private int totalRestows;
    private List<RestowMovimentoDto> movimentos;
    private String descricao;

    public RestowAnaliseDto() {
    }

    public int getTotalRestows() {
        return totalRestows;
    }

    public void setTotalRestows(int totalRestows) {
        this.totalRestows = totalRestows;
    }

    public List<RestowMovimentoDto> getMovimentos() {
        return movimentos;
    }

    public void setMovimentos(List<RestowMovimentoDto> movimentos) {
        this.movimentos = movimentos;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
