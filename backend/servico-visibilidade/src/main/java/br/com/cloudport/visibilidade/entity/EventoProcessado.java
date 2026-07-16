package br.com.cloudport.visibilidade.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "visibilidade_evento_processado")
public class EventoProcessado {

    @Id
    @Column(name = "identidade_evento", nullable = false, length = 150)
    private String identidadeEvento;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(name = "hash_payload", nullable = false, length = 64)
    private String hashPayload;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;

    public String getIdentidadeEvento() {
        return identidadeEvento;
    }

    public void setIdentidadeEvento(String identidadeEvento) {
        this.identidadeEvento = identidadeEvento;
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

    public void setProcessadoEm(LocalDateTime processadoEm) {
        this.processadoEm = processadoEm;
    }
}
