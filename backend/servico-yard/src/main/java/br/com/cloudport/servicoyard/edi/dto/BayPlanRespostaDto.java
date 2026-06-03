package br.com.cloudport.servicoyard.edi.dto;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BayPlanRespostaDto {

    private Long id;
    private String codigoNavio;
    private String nomeNavio;
    private String codigoViagem;
    private String portoCarga;
    private String portoDescarga;
    private StatusBayPlan status;
    private String origemMensagem;
    private LocalDateTime atualizadoEm;
    private Long versao;
    private int totalContainers;
    private int totalCarregamento;
    private int totalDescarga;
    private List<BayPlanContainerDto> containers;

    public static BayPlanRespostaDto deEntidade(BayPlan bp) {
        BayPlanRespostaDto dto = new BayPlanRespostaDto();
        dto.id = bp.getId();
        dto.codigoNavio = bp.getCodigoNavio();
        dto.nomeNavio = bp.getNomeNavio();
        dto.codigoViagem = bp.getCodigoViagem();
        dto.portoCarga = bp.getPortoCarga();
        dto.portoDescarga = bp.getPortoDescarga();
        dto.status = bp.getStatus();
        dto.origemMensagem = bp.getOrigemMensagem();
        dto.atualizadoEm = bp.getAtualizadoEm();
        dto.versao = bp.getVersao();
        dto.containers = bp.getContainers().stream()
                .map(BayPlanContainerDto::deEntidade)
                .collect(Collectors.toList());
        dto.totalContainers = dto.containers.size();
        dto.totalCarregamento = (int) bp.getContainers().stream()
                .filter(c -> c.getTipoOperacao() == TipoOperacaoBayPlan.CARREGAMENTO).count();
        dto.totalDescarga = (int) bp.getContainers().stream()
                .filter(c -> c.getTipoOperacao() == TipoOperacaoBayPlan.DESCARGA).count();
        return dto;
    }

    public Long getId() { return id; }
    public String getCodigoNavio() { return codigoNavio; }
    public String getNomeNavio() { return nomeNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public String getPortoCarga() { return portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public StatusBayPlan getStatus() { return status; }
    public String getOrigemMensagem() { return origemMensagem; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Long getVersao() { return versao; }
    public int getTotalContainers() { return totalContainers; }
    public int getTotalCarregamento() { return totalCarregamento; }
    public int getTotalDescarga() { return totalDescarga; }
    public List<BayPlanContainerDto> getContainers() { return containers; }
}
