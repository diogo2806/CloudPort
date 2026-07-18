package br.com.cloudport.servicoyard.patio.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "work_instruction")
public class InstrucaoTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_conteiner", nullable = false, length = 30)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 30)
    private TipoOperacaoInstrucao tipoOperacao;

    @Column(name = "origem", length = 80)
    private String origem;

    @Column(name = "destino", length = 80)
    private String destino;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 20)
    private PrioridadeInstrucao prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusInstrucao status;

    @Column(name = "agendada_em")
    private LocalDateTime agendadaEm;

    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "equipamento", length = 30)
    private String equipamento;

    @Column(name = "equipe", length = 80)
    private String equipe;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "criado_por", nullable = false, length = 80)
    private String criadoPor;

    @Column(name = "justificativa_cancelamento", length = 500)
    private String justificativaCancelamento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void criarAuditoria() {
        LocalDateTime agora = LocalDateTime.now();
        createdAt = agora;
        updatedAt = agora;
    }

    @PreUpdate
    public void atualizarAuditoria() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCodigoConteiner() { return codigoConteiner; }
    public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
    public TipoOperacaoInstrucao getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoInstrucao tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public PrioridadeInstrucao getPrioridade() { return prioridade; }
    public void setPrioridade(PrioridadeInstrucao prioridade) { this.prioridade = prioridade; }
    public StatusInstrucao getStatus() { return status; }
    public void setStatus(StatusInstrucao status) { this.status = status; }
    public LocalDateTime getAgendadaEm() { return agendadaEm; }
    public void setAgendadaEm(LocalDateTime agendadaEm) { this.agendadaEm = agendadaEm; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(LocalDateTime iniciadaEm) { this.iniciadaEm = iniciadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }
    public LocalDateTime getCanceladaEm() { return canceladaEm; }
    public void setCanceladaEm(LocalDateTime canceladaEm) { this.canceladaEm = canceladaEm; }
    public String getEquipamento() { return equipamento; }
    public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
    public String getEquipe() { return equipe; }
    public void setEquipe(String equipe) { this.equipe = equipe; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) { this.justificativaCancelamento = justificativaCancelamento; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
