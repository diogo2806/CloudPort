package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import javax.validation.constraints.NotBlank;

public class AtualizacaoWorkQueueRecursosDto {

    private Integer porao;
    private Long planoGuindasteId;
    private Long recursoCaisId;
    private Long equipamentoPatioId;
    @NotBlank
    private String motivo;
    private String usuario;
    private String origemAcao;
    private String correlationId;

    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public Long getPlanoGuindasteId() { return planoGuindasteId; }
    public void setPlanoGuindasteId(Long planoGuindasteId) { this.planoGuindasteId = planoGuindasteId; }
    public Long getRecursoCaisId() { return recursoCaisId; }
    public void setRecursoCaisId(Long recursoCaisId) { this.recursoCaisId = recursoCaisId; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getOrigemAcao() { return origemAcao; }
    public void setOrigemAcao(String origemAcao) { this.origemAcao = origemAcao; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
