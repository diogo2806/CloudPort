package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.List;

public class SequenciamentoGuindasteDto {

    private int totalOperacoes;
    private int numGuindastes;
    private List<GuindasteOperacaoDto> sequencia;

    public SequenciamentoGuindasteDto() {
    }

    public int getTotalOperacoes() {
        return totalOperacoes;
    }

    public void setTotalOperacoes(int totalOperacoes) {
        this.totalOperacoes = totalOperacoes;
    }

    public int getNumGuindastes() {
        return numGuindastes;
    }

    public void setNumGuindastes(int numGuindastes) {
        this.numGuindastes = numGuindastes;
    }

    public List<GuindasteOperacaoDto> getSequencia() {
        return sequencia;
    }

    public void setSequencia(List<GuindasteOperacaoDto> sequencia) {
        this.sequencia = sequencia;
    }
}
