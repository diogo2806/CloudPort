package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;

public class DispatchWorkQueueDto {

    private List<Long> ordemIds;
    private Boolean somentePendentes;
    private Integer limiteOrdens;
    private String operador;
    private String usuario;
    private String motivo;
    private String observacao;
    private String origemAcao;
    private String correlationId;

    public List<Long> getOrdemIds() { return ordemIds; }
    public void setOrdemIds(List<Long> ordemIds) { this.ordemIds = ordemIds; }
    public Boolean getSomentePendentes() { return somentePendentes; }
    public void setSomentePendentes(Boolean somentePendentes) { this.somentePendentes = somentePendentes; }
    public Integer getLimiteOrdens() { return limiteOrdens; }
    public void setLimiteOrdens(Integer limiteOrdens) { this.limiteOrdens = limiteOrdens; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getOrigemAcao() { return origemAcao; }
    public void setOrigemAcao(String origemAcao) { this.origemAcao = origemAcao; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public boolean somentePendentesEfetivo() {
        return somentePendentes == null || somentePendentes;
    }

    public int limiteOrdensEfetivo() {
        return limiteOrdens == null || limiteOrdens <= 0 ? Integer.MAX_VALUE : limiteOrdens;
    }

    public String motivoEfetivo() {
        if (motivo != null && !motivo.isBlank()) {
            return motivo.trim();
        }
        return observacao != null && !observacao.isBlank() ? observacao.trim() : null;
    }

    public String usuarioEfetivo() {
        if (operador != null && !operador.isBlank()) {
            return operador.trim();
        }
        if (usuario != null && !usuario.isBlank()) {
            return usuario.trim();
        }
        return "sistema";
    }
}
