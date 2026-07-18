package br.com.cloudport.servicoyard.patio.modelo;

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
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "vmt_instruction_event")
public class EventoInstrucaoVmt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 120)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instruction_id", nullable = false)
    private InstrucaoTrabalho instrucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private TipoEventoVmt tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_status", nullable = false, length = 30)
    private StatusInstrucao statusEsperado;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(name = "result", length = 1000)
    private String resultado;

    @Column(name = "payload")
    private String payload;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processadoEm;

    @PrePersist
    public void preencherProcessamento() {
        if (processadoEm == null) {
            processadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public InstrucaoTrabalho getInstrucao() { return instrucao; }
    public void setInstrucao(InstrucaoTrabalho instrucao) { this.instrucao = instrucao; }
    public TipoEventoVmt getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoVmt tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusInstrucao getStatusEsperado() { return statusEsperado; }
    public void setStatusEsperado(StatusInstrucao statusEsperado) { this.statusEsperado = statusEsperado; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(LocalDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public LocalDateTime getProcessadoEm() { return processadoEm; }
}
