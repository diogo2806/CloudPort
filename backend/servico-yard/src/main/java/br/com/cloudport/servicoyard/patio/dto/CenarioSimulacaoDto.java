package br.com.cloudport.servicoyard.patio.dto;

import java.io.Serializable;

public class CenarioSimulacaoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TipoCenario {
        ATRASO_NAVIO,
        MANUTENCAO_EQUIPAMENTO,
        AUMENTO_VOLUME
    }

    private TipoCenario tipoCenario;
    private String descricao;
    private Integer horasAtraso;
    private String codigoEquipamento;
    private Integer quantidadeConteinoresAdicionais;

    public CenarioSimulacaoDto() {
    }

    public CenarioSimulacaoDto(TipoCenario tipoCenario, String descricao,
                               Integer horasAtraso, String codigoEquipamento,
                               Integer quantidadeConteinoresAdicionais) {
        this.tipoCenario = tipoCenario;
        this.descricao = descricao;
        this.horasAtraso = horasAtraso;
        this.codigoEquipamento = codigoEquipamento;
        this.quantidadeConteinoresAdicionais = quantidadeConteinoresAdicionais;
    }

    public TipoCenario getTipoCenario() {
        return tipoCenario;
    }

    public void setTipoCenario(TipoCenario tipoCenario) {
        this.tipoCenario = tipoCenario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getHorasAtraso() {
        return horasAtraso;
    }

    public void setHorasAtraso(Integer horasAtraso) {
        this.horasAtraso = horasAtraso;
    }

    public String getCodigoEquipamento() {
        return codigoEquipamento;
    }

    public void setCodigoEquipamento(String codigoEquipamento) {
        this.codigoEquipamento = codigoEquipamento;
    }

    public Integer getQuantidadeConteinoresAdicionais() {
        return quantidadeConteinoresAdicionais;
    }

    public void setQuantidadeConteinoresAdicionais(Integer quantidadeConteinoresAdicionais) {
        this.quantidadeConteinoresAdicionais = quantidadeConteinoresAdicionais;
    }
}
