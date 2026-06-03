package br.com.cloudport.servicoyard.edi.dto;

import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento WebSocket publicado em /topico/edi/bay-plan/{codigoNavio}
 * quando o Bay Plan é criado ou alterado por mensagem EDI.
 */
public class AtualizacaoBayPlanEventoDto {

    private Long bayPlanId;
    private String codigoNavio;
    private String codigoViagem;
    private TipoMensagemEdi tipoMensagem;
    private StatusBayPlan novoStatus;
    private List<String> containersAdicionados;
    private List<String> containersAtualizados;
    private int totalContainers;
    private LocalDateTime timestamp;

    public AtualizacaoBayPlanEventoDto() {
        this.timestamp = LocalDateTime.now();
    }

    public Long getBayPlanId() { return bayPlanId; }
    public void setBayPlanId(Long bayPlanId) { this.bayPlanId = bayPlanId; }
    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public TipoMensagemEdi getTipoMensagem() { return tipoMensagem; }
    public void setTipoMensagem(TipoMensagemEdi tipoMensagem) { this.tipoMensagem = tipoMensagem; }
    public StatusBayPlan getNovoStatus() { return novoStatus; }
    public void setNovoStatus(StatusBayPlan novoStatus) { this.novoStatus = novoStatus; }
    public List<String> getContainersAdicionados() { return containersAdicionados; }
    public void setContainersAdicionados(List<String> containersAdicionados) { this.containersAdicionados = containersAdicionados; }
    public List<String> getContainersAtualizados() { return containersAtualizados; }
    public void setContainersAtualizados(List<String> containersAtualizados) { this.containersAtualizados = containersAtualizados; }
    public int getTotalContainers() { return totalContainers; }
    public void setTotalContainers(int totalContainers) { this.totalContainers = totalContainers; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
