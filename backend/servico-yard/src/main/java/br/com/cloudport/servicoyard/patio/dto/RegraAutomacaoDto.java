package br.com.cloudport.servicoyard.patio.dto;

import java.io.Serializable;

public class RegraAutomacaoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TipoRegra {
        AGRUPAR_POR_DESTINO,
        MINIMIZAR_REHANDLE,
        ALOCAR_REEFER_COM_ENERGIA,
        ALOCAR_CARGA_PERIGOSA_AREA_RESTRITA
    }

    private Long id;
    private TipoRegra tipoRegra;
    private String descricao;
    private Boolean ativa;
    private Integer prioridade;

    public RegraAutomacaoDto() {
    }

    public RegraAutomacaoDto(TipoRegra tipoRegra, String descricao, Boolean ativa, Integer prioridade) {
        this.tipoRegra = tipoRegra;
        this.descricao = descricao;
        this.ativa = ativa != null ? ativa : true;
        this.prioridade = prioridade != null ? prioridade : 0;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoRegra getTipoRegra() { return tipoRegra; }
    public void setTipoRegra(TipoRegra tipoRegra) { this.tipoRegra = tipoRegra; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getAtiva() { return ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }

    public Integer getPrioridade() { return prioridade; }
    public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
}
