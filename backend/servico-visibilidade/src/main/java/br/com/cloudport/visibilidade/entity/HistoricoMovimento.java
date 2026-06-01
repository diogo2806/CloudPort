package br.com.cloudport.visibilidade.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_movimento")
public class HistoricoMovimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "container_id", nullable = false)
    private String containerId;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "tipo")
    private String tipo; // ENTRADA_GATE, ARMAZENAGEM_YARD, etc.

    @Column(name = "localizacao")
    private String localizacao;

    @Column(name = "responsavel")
    private String responsavel;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "equipamento_usado")
    private String equipamentoUsado;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getEquipamentoUsado() { return equipamentoUsado; }
    public void setEquipamentoUsado(String equipamentoUsado) { this.equipamentoUsado = equipamentoUsado; }
}