package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;

public class DispatchWorkQueueDto {

    private List<Long> ordemIds;
    private Boolean somentePendentes;
    private String usuario;

    public List<Long> getOrdemIds() { return ordemIds; }
    public void setOrdemIds(List<Long> ordemIds) { this.ordemIds = ordemIds; }
    public Boolean getSomentePendentes() { return somentePendentes; }
    public void setSomentePendentes(Boolean somentePendentes) { this.somentePendentes = somentePendentes; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public boolean somentePendentesEfetivo() {
        return somentePendentes == null || somentePendentes;
    }
}
