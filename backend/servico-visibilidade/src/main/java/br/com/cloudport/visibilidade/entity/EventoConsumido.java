package br.com.cloudport.visibilidade.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "evento_consumido")
public class EventoConsumido {

    @Id
    @Column(name = "evento_id", nullable = false, length = 150)
    private String eventoId;

    @Column(nullable = false, length = 40)
    private String origem;

    @Column(name = "tipo_evento", nullable = false, length = 120)
    private String tipoEvento;

    @Column(name = "hash_payload", nullable = false, length = 64)
    private String hashPayload;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;

    @PrePersist
    void prePersist() {
        if (processadoEm == null) {
            processadoEm = LocalDateTime.now();
        }
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getHashPayload() {
        return hashPayload;
    }

    public void setHashPayload(String hashPayload) {
        this.hashPayload = hashPayload;
    }

    public LocalDateTime getProcessadoEm() {
        return processadoEm;
    }
}
