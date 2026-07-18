package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import br.com.cloudport.servicogate.model.enums.GateQueuePriority;
import br.com.cloudport.servicogate.model.enums.GateQueueStatus;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "gate_queue_entry")
public class GateQueueEntry extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "gate_pass_id", nullable = false)
    private GatePass gatePass;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentido", nullable = false, length = 10)
    private GateQueueDirection sentido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GateQueueStatus status;

    @Column(name = "posicao_original", nullable = false)
    private Integer posicaoOriginal;

    @Column(name = "posicao_atual", nullable = false)
    private Integer posicaoAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 20)
    private GateQueuePriority prioridade;

    @Column(name = "justificativa_prioridade", length = 500)
    private String justificativaPrioridade;

    @Column(name = "operador_prioridade", length = 80)
    private String operadorPrioridade;

    @Column(name = "entrou_em", nullable = false)
    private LocalDateTime entrouEm;

    @Column(name = "chamado_em")
    private LocalDateTime chamadoEm;

    @Column(name = "atendimento_iniciado_em")
    private LocalDateTime atendimentoIniciadoEm;

    public Long getId() { return id; }
    public GatePass getGatePass() { return gatePass; }
    public void setGatePass(GatePass gatePass) { this.gatePass = gatePass; }
    public GateQueueDirection getSentido() { return sentido; }
    public void setSentido(GateQueueDirection sentido) { this.sentido = sentido; }
    public GateQueueStatus getStatus() { return status; }
    public void setStatus(GateQueueStatus status) { this.status = status; }
    public Integer getPosicaoOriginal() { return posicaoOriginal; }
    public void setPosicaoOriginal(Integer posicaoOriginal) { this.posicaoOriginal = posicaoOriginal; }
    public Integer getPosicaoAtual() { return posicaoAtual; }
    public void setPosicaoAtual(Integer posicaoAtual) { this.posicaoAtual = posicaoAtual; }
    public GateQueuePriority getPrioridade() { return prioridade; }
    public void setPrioridade(GateQueuePriority prioridade) { this.prioridade = prioridade; }
    public String getJustificativaPrioridade() { return justificativaPrioridade; }
    public void setJustificativaPrioridade(String justificativaPrioridade) { this.justificativaPrioridade = justificativaPrioridade; }
    public String getOperadorPrioridade() { return operadorPrioridade; }
    public void setOperadorPrioridade(String operadorPrioridade) { this.operadorPrioridade = operadorPrioridade; }
    public LocalDateTime getEntrouEm() { return entrouEm; }
    public void setEntrouEm(LocalDateTime entrouEm) { this.entrouEm = entrouEm; }
    public LocalDateTime getChamadoEm() { return chamadoEm; }
    public void setChamadoEm(LocalDateTime chamadoEm) { this.chamadoEm = chamadoEm; }
    public LocalDateTime getAtendimentoIniciadoEm() { return atendimentoIniciadoEm; }
    public void setAtendimentoIniciadoEm(LocalDateTime atendimentoIniciadoEm) { this.atendimentoIniciadoEm = atendimentoIniciadoEm; }
}
