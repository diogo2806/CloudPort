package br.com.cloudport.visibilidade.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "evento_processado",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evento_processado_identidade",
                columnNames = "identidade_evento"))
public class EventoProcessado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identidade_evento", nullable = false, length = 150)
    private String identidadeEvento;

    @Column(name = "tipo_evento", nullable = false, length = 150)
    private String tipoEvento;

    @Column(name = "versao_evento", nullable = false)
    private Integer versaoEvento;

    @Column(name = "consumidor", nullable = false, length = 50)
    private String consumidor;

    @Column(name = "origem_evento", length = 100)
    private String origemEvento;

    @Column(name = "hash_payload", nullable = false, length = 64)
    private String hashPayload;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getVersaoEvento() {
        return versaoEvento;
    }

    public void setVersaoEvento(Integer versaoEvento) {
        this.versaoEvento = versaoEvento;
    }

    public String getConsumidor() {
        return consumidor;
    }

    public void setConsumidor(String consumidor) {
        this.consumidor = consumidor;
    }

    public String getOrigemEvento() {
        return origemEvento;
    }

    public void setOrigemEvento(String origemEvento) {
        this.origemEvento = origemEvento;
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
