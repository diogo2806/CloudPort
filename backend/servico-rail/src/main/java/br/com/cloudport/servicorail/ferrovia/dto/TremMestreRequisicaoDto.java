package br.com.cloudport.servicorail.ferrovia.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TremMestreRequisicaoDto {
    @NotBlank @Size(max = 40) private String identificador;
    @NotBlank @Size(max = 80) private String operadoraFerroviaria;
    @Size(max = 120) private String descricao;
    private boolean ativo = true;
    @Size(max = 500) private String observacoes;
    @Valid private List<VagaoVisitaRequisicaoDto> composicaoPadrao = new ArrayList<>();

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public void setOperadoraFerroviaria(String valor) { this.operadoraFerroviaria = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public List<VagaoVisitaRequisicaoDto> getComposicaoPadrao() { return composicaoPadrao; }
    public void setComposicaoPadrao(List<VagaoVisitaRequisicaoDto> lista) { this.composicaoPadrao = lista != null ? new ArrayList<>(lista) : new ArrayList<>(); }
}
