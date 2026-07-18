package br.com.cloudport.servicoyard.patio.listatrabalho.modelo;

import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "evento_vmt_work_instruction")
public class EventoVmtWorkInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(name = "ordem_trabalho_patio_id", nullable = false)
    private Long ordemTrabalhoPatioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoVmt tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_esperado", nullable = false, length = 30)
    private StatusConfirmacaoVmt statusEsperado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_resultante", nullable = false, length = 30)
    private StatusConfirmacaoVmt statusResultante;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(name = "resultado", length = 1000)
    private String resultado;

    @Column(name = "payload")
    private String payload;

    @Column(name = "processado_em", nullable = false)
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
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public TipoEventoVmt getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoVmt tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusConfirmacaoVmt getStatusEsperado() { return statusEsperado; }
    public void setStatusEsperado(StatusConfirmacaoVmt statusEsperado) { this.statusEsperado = statusEsperado; }
    public StatusConfirmacaoVmt getStatusResultante() { return statusResultante; }
    public void setStatusResultante(StatusConfirmacaoVmt statusResultante) { this.statusResultante = statusResultante; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(LocalDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public LocalDateTime getProcessadoEm() { return processadoEm; }
}
