package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;

public class AtualizacaoWorkQueueOrdensDto {

    private List<Long> ordemIds;

    public List<Long> getOrdemIds() {
        return ordemIds;
    }

    public void setOrdemIds(List<Long> ordemIds) {
        this.ordemIds = ordemIds;
    }
}
