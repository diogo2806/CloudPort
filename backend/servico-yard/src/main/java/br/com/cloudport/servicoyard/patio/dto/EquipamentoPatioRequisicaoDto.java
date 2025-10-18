package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class EquipamentoPatioRequisicaoDto {

    private Long id;

    @NotBlank
    @Size(max = 30)
    private String identificador;

    @NotNull
    private TipoEquipamento tipoEquipamento;

    @NotNull
    @Min(0)
    private Integer linha;

    @NotNull
    @Min(0)
    private Integer coluna;

    @NotNull
    private StatusEquipamento statusOperacional;

    public EquipamentoPatioRequisicaoDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = ValidacaoEntradaUtil.limparTexto(identificador);
    }

    public TipoEquipamento getTipoEquipamento() {
        return tipoEquipamento;
    }

    public void setTipoEquipamento(TipoEquipamento tipoEquipamento) {
        this.tipoEquipamento = tipoEquipamento;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public StatusEquipamento getStatusOperacional() {
        return statusOperacional;
    }

    public void setStatusOperacional(StatusEquipamento statusOperacional) {
        this.statusOperacional = statusOperacional;
    }
}
