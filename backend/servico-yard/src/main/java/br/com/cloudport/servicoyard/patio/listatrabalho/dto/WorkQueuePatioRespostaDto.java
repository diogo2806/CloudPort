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
        dto.id = fila.getId();
        dto.identificador = escapar(fila.getIdentificador());
        dto.agrupamento = fila.getId() == null ? "DERIVADA_VISITA_BERCO_ZONA_STATUS" : "WORK_QUEUE_PATIO";
        dto.visitaNavioId = fila.getVisitaNavioId();
        dto.berco = escapar(fila.getBerco());
        dto.porao = fila.getPorao();
        dto.planoGuindasteId = fila.getPlanoGuindasteId();
        dto.recursoCaisId = fila.getRecursoCaisId();
        dto.blocoZona = escapar(fila.getBlocoZona());
        dto.sequenciaInicial = fila.getSequenciaInicial() != null ? fila.getSequenciaInicial() : sequenciaInicial(ordens);
        dto.pow = escapar(fila.getPow());
        dto.poolOperacional = escapar(fila.getPoolOperacional());
        dto.equipamento = escapar(fila.getEquipamento());
        dto.equipamentoPatioId = fila.getEquipamentoPatioId();
        dto.status = fila.getStatus() == null ? null : fila.getStatus().name();
        dto.prioridadeOperacional = fila.getPrioridadeOperacional();
        dto.totalOrdens = ordens.size();
        dto.jobList = ordens.stream().map(OrdemTrabalhoPatioRespostaDto::deEntidade).toList();
        dto.criadoEm = fila.getCriadoEm();
        dto.atualizadoEm = fila.getAtualizadoEm();
        return dto;
    }

    private static Integer sequenciaInicial(List<OrdemTrabalhoPatio> ordens) {
        return ordens.stream().map(OrdemTrabalhoPatio::getSequenciaNavio).filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
    }

    private static String escapar(String valor) {
        return valor == null ? null : HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    public Long getId() { return id; }
    public String getIdentificador() { return identificador; }
    public String getAgrupamento() { return agrupamento; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public String getBerco() { return berco; }
    public Integer getPorao() { return porao; }
    public Long getPlanoGuindasteId() { return planoGuindasteId; }
    public Long getRecursoCaisId() { return recursoCaisId; }
    public String getBlocoZona() { return blocoZona; }
    public Integer getSequenciaInicial() { return sequenciaInicial; }
    public String getPow() { return pow; }
    public String getPoolOperacional() { return poolOperacional; }
    public String getEquipamento() { return equipamento; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public String getStatus() { return status; }
    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public int getTotalOrdens() { return totalOrdens; }
    public List<OrdemTrabalhoPatioRespostaDto> getJobList() { return jobList; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
