package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.GateCallPriority;
import br.com.cloudport.servicogate.model.enums.GateCallStatus;
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
@Table(name = "gate_call")
public class GateCall extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "gate_pass_id", nullable = false)
    private GatePass gatePass;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GateCallStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 20)
    private GateCallPriority prioridade;

    @Column(name = "posicao_fila", nullable = false)
    private Integer posicaoFila;

    @Column(name = "gate_pista", nullable = false, length = 80)
    private String gatePista;

    @Column(name = "chamado_em", nullable = false)
    private LocalDateTime chamadoEm;

    @Column(name = "aceito_em")
    private LocalDateTime aceitoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "expirado_em")
    private LocalDateTime expiradoEm;

    @Column(name = "atendimento_iniciado_em")
    private LocalDateTime atendimentoIniciadoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "quantidade_rechamadas", nullable = false)
    private Integer quantidadeRechamadas = 0;

    @Column(name = "ultima_rechamada_em")
    private LocalDateTime ultimaRechamadaEm;

    @Column(name = "justificativa_cancelamento", length = 500)
    private String justificativaCancelamento;

    @Column(name = "operador", length = 80)
    private String operador;

    public Long getId() { return id; }
    public GatePass getGatePass() { return gatePass; }
    public void setGatePass(GatePass gatePass) { this.gatePass = gatePass; }
    public GateCallStatus getStatus() { return status; }
    public void setStatus(GateCallStatus status) { this.status = status; }
    public GateCallPriority getPrioridade() { return prioridade; }
    public void setPrioridade(GateCallPriority prioridade) { this.prioridade = prioridade; }
    public Integer getPosicaoFila() { return posicaoFila; }
    public void setPosicaoFila(Integer posicaoFila) { this.posicaoFila = posicaoFila; }
    public String getGatePista() { return gatePista; }
    public void setGatePista(String gatePista) { this.gatePista = gatePista; }
    public LocalDateTime getChamadoEm() { return chamadoEm; }
    public void setChamadoEm(LocalDateTime chamadoEm) { this.chamadoEm = chamadoEm; }
    public LocalDateTime getAceitoEm() { return aceitoEm; }
    public void setAceitoEm(LocalDateTime aceitoEm) { this.aceitoEm = aceitoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public LocalDateTime getExpiradoEm() { return expiradoEm; }
    public void setExpiradoEm(LocalDateTime expiradoEm) { this.expiradoEm = expiradoEm; }
    public LocalDateTime getAtendimentoIniciadoEm() { return atendimentoIniciadoEm; }
    public void setAtendimentoIniciadoEm(LocalDateTime atendimentoIniciadoEm) { this.atendimentoIniciadoEm = atendimentoIniciadoEm; }
    public LocalDateTime getFinalizadoEm() { return finalizadoEm; }
    public void setFinalizadoEm(LocalDateTime finalizadoEm) { this.finalizadoEm = finalizadoEm; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime canceladoEm) { this.canceladoEm = canceladoEm; }
    public Integer getQuantidadeRechamadas() { return quantidadeRechamadas; }
    public void setQuantidadeRechamadas(Integer quantidadeRechamadas) { this.quantidadeRechamadas = quantidadeRechamadas; }
    public LocalDateTime getUltimaRechamadaEm() { return ultimaRechamadaEm; }
    public void setUltimaRechamadaEm(LocalDateTime ultimaRechamadaEm) { this.ultimaRechamadaEm = ultimaRechamadaEm; }
    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) { this.justificativaCancelamento = justificativaCancelamento; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
}
