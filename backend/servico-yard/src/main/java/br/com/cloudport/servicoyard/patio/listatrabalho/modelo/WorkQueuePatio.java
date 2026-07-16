package br.com.cloudport.servicoyard.patio.listatrabalho.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "work_queue_patio")
public class WorkQueuePatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador", nullable = false, unique = true, length = 120)
    private String identificador;

    @Column(name = "visita_navio_id", nullable = false)
    private Long visitaNavioId;

    @Column(name = "berco", length = 60)
    private String berco;

    @Column(name = "porao")
    private Integer porao;

    @Column(name = "plano_guindaste_id")
    private Long planoGuindasteId;

    @Column(name = "recurso_cais_id")
    private Long recursoCaisId;

    @Column(name = "bloco_zona", length = 60)
    private String blocoZona;

    @Column(name = "sequencia_inicial")
    private Integer sequenciaInicial;

    @Column(name = "pow", length = 80)
    private String pow;

    @Column(name = "pool_operacional", length = 80)
    private String poolOperacional;

    @Column(name = "equipamento", length = 80)
    private String equipamento;

    @Column(name = "equipamento_patio_id")
    private Long equipamentoPatioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusWorkQueuePatio status;

    @Column(name = "prioridade_operacional")
    private Integer prioridadeOperacional;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
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
    public StatusWorkQueuePatio getStatus() { return status; }
    public void setStatus(StatusWorkQueuePatio status) { this.status = status; }
    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
