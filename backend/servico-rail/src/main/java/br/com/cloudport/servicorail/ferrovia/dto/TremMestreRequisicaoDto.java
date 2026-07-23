package br.com.cloudport.servicorail.ferrovia.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TremMestreRequisicaoDto {

    @NotBlank
    @Size(max = 40)
    private String identificador;

    @NotBlank
    @Size(max = 80)
    private String operadoraFerroviaria;

    @NotBlank
    @Size(max = 120)
    private String nomeOperacional;

    @Size(max = 1000)
    private String observacoes;

    private boolean ativo = true;

    @Valid
    private List<VagaoVisitaRequisicaoDto> composicaoPadrao = new ArrayList<>();

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public void setOperadoraFerroviaria(String operadoraFerroviaria) { this.operadoraFerroviaria = operadoraFerroviaria; }
    public String getNomeOperacional() { return nomeOperacional; }
    public void setNomeOperacional(String nomeOperacional) { this.nomeOperacional = nomeOperacional; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public List<VagaoVisitaRequisicaoDto> getComposicaoPadrao() { return composicaoPadrao; }
    public void setComposicaoPadrao(List<VagaoVisitaRequisicaoDto> composicaoPadrao) {
        this.composicaoPadrao = composicaoPadrao != null ? new ArrayList<>(composicaoPadrao) : new ArrayList<>();
    }
}
