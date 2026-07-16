package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.util.HtmlUtils;

public class WorkQueuePatioRespostaDto {

    private Long id;
    private String identificador;
    private String agrupamento;
    private Long visitaNavioId;
    private String berco;
    private Integer porao;
    private Long planoGuindasteId;
    private Long recursoCaisId;
    private String blocoZona;
    private Integer sequenciaInicial;
    private String pow;
    private String poolOperacional;
    private String equipamento;
    private Long equipamentoPatioId;
    private String status;
    private Integer prioridadeOperacional;
    private int totalOrdens;
    private List<OrdemTrabalhoPatioRespostaDto> jobList;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static WorkQueuePatioRespostaDto deEntidade(WorkQueuePatio fila, List<OrdemTrabalhoPatio> ordens) {
        WorkQueuePatioRespostaDto dto = new WorkQueuePatioRespostaDto();
        dto.setId(fila.getId());
        dto.setIdentificador(escapar(fila.getIdentificador()));
        dto.setAgrupamento(fila.getId() == null ? "DERIVADA_VISITA_BERCO_ZONA_STATUS" : "WORK_QUEUE_PATIO");
        dto.setVisitaNavioId(fila.getVisitaNavioId());
        dto.setBerco(escapar(fila.getBerco()));
        dto.setPorao(fila.getPorao());
        dto.setPlanoGuindasteId(fila.getPlanoGuindasteId());
        dto.setRecursoCaisId(fila.getRecursoCaisId());
        dto.setBlocoZona(escapar(fila.getBlocoZona()));
        dto.setSequenciaInicial(fila.getSequenciaInicial() != null ? fila.getSequenciaInicial() : sequenciaInicial(ordens));
        dto.setPow(escapar(fila.getPow()));
        dto.setPoolOperacional(escapar(fila.getPoolOperacional()));
        dto.setEquipamento(escapar(fila.getEquipamento()));
        dto.setEquipamentoPatioId(fila.getEquipamentoPatioId());
        dto.setStatus(fila.getStatus() == null ? null : fila.getStatus().name());
        dto.setPrioridadeOperacional(fila.getPrioridadeOperacional());
        dto.setTotalOrdens(ordens.size());
        dto.setJobList(ordens.stream().map(OrdemTrabalhoPatioRespostaDto::deEntidade).toList());
        dto.setCriadoEm(fila.getCriadoEm());
        dto.setAtualizadoEm(fila.getAtualizadoEm());
        return dto;
    }

    private static Integer sequenciaInicial(List<OrdemTrabalhoPatio> ordens) {
        return ordens.stream()
                .map(OrdemTrabalhoPatio::getSequenciaNavio)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getAgrupamento() { return agrupamento; }
    public void setAgrupamento(String agrupamento) { this.agrupamento = agrupamento; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public Long getPlanoGuindasteId() { return planoGuindasteId; }
    public void setPlanoGuindasteId(Long planoGuindasteId) { this.planoGuindasteId = planoGuindasteId; }
    public Long getRecursoCaisId() { return recursoCaisId; }
    public void setRecursoCaisId(Long recursoCaisId) { this.recursoCaisId = recursoCaisId; }
    public String getBlocoZona() { return blocoZona; }
    public void setBlocoZona(String blocoZona) { this.blocoZona = blocoZona; }
    public Integer getSequenciaInicial() { return sequenciaInicial; }
    public void setSequenciaInicial(Integer sequenciaInicial) { this.sequenciaInicial = sequenciaInicial; }
    public String getPow() { return pow; }
    public void setPow(String pow) { this.pow = pow; }
    public String getPoolOperacional() { return poolOperacional; }
    public void setPoolOperacional(String poolOperacional) { this.poolOperacional = poolOperacional; }
    public String getEquipamento() { return equipamento; }
    public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    public int getTotalOrdens() { return totalOrdens; }
    public void setTotalOrdens(int totalOrdens) { this.totalOrdens = totalOrdens; }
    public List<OrdemTrabalhoPatioRespostaDto> getJobList() { return jobList; }
    public void setJobList(List<OrdemTrabalhoPatioRespostaDto> jobList) { this.jobList = jobList; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
