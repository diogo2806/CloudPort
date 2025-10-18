package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;

public class EquipamentoMapaDto {

    private Long id;
    private String identificador;
    private TipoEquipamento tipoEquipamento;
    private Integer linha;
    private Integer coluna;
    private StatusEquipamento statusOperacional;

    public EquipamentoMapaDto() {
    }

    public EquipamentoMapaDto(Long id, String identificador, TipoEquipamento tipoEquipamento,
                               Integer linha, Integer coluna, StatusEquipamento statusOperacional) {
        this.id = id;
        this.identificador = identificador;
        this.tipoEquipamento = tipoEquipamento;
        this.linha = linha;
        this.coluna = coluna;
        this.statusOperacional = statusOperacional;
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
        this.identificador = identificador;
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
