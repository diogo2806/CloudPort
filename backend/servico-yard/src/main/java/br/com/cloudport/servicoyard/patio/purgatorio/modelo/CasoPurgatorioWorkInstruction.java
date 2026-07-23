package br.com.cloudport.servicoyard.patio.purgatorio.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "caso_purgatorio_work_instruction",
        uniqueConstraints = @UniqueConstraint(name = "uk_purgatorio_chave_idempotencia", columnNames = "chave_idempotencia"))
public class CasoPurgatorioWorkInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_trabalho_patio_id", nullable = false)
    private Long ordemTrabalhoPatioId;

    @Column(name = "work_queue_id", nullable = false)
    private Long workQueueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "causa", nullable = false, length = 30)
    private CausaPurgatorioWorkInstruction causa;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private SeveridadePurgatorioWorkInstruction severidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPurgatorioWorkInstruction estado;

    @Column(name = "chave_idempotencia", nullable = false, length = 120)
    private String chaveIdempotencia;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "origem", length = 120)
    private String origem;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Lob
    @Column(name = "snapshot_original", nullable = false)
    private String snapshotOriginal;

    @Lob
    @Column(name = "snapshot_atual")
    private String snapshotAtual;

    @Lob
    @Column(name = "evidencias")
    private String evidencias;

    @Lob
    @Column(name = "historico", nullable = false)
    private String historico;

    @Column(name = "resolucao", length = 500)
    private String resolucao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public CausaPurgatorioWorkInstruction getCausa() { return causa; }
    public void setCausa(CausaPurgatorioWorkInstruction causa) { this.causa = causa; }
    public SeveridadePurgatorioWorkInstruction getSeveridade() { return severidade; }
    public void setSeveridade(SeveridadePurgatorioWorkInstruction severidade) { this.severidade = severidade; }
    public EstadoPurgatorioWorkInstruction getEstado() { return estado; }
    public void setEstado(EstadoPurgatorioWorkInstruction estado) { this.estado = estado; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getSnapshotOriginal() { return snapshotOriginal; }
    public void setSnapshotOriginal(String snapshotOriginal) { this.snapshotOriginal = snapshotOriginal; }
    public String getSnapshotAtual() { return snapshotAtual; }
    public void setSnapshotAtual(String snapshotAtual) { this.snapshotAtual = snapshotAtual; }
    public String getEvidencias() { return evidencias; }
    public void setEvidencias(String evidencias) { this.evidencias = evidencias; }
    public String getHistorico() { return historico; }
    public void setHistorico(String historico) { this.historico = historico; }
    public String getResolucao() { return resolucao; }
    public void setResolucao(String resolucao) { this.resolucao = resolucao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public LocalDateTime getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(LocalDateTime resolvidoEm) { this.resolvidoEm = resolvidoEm; }
}
