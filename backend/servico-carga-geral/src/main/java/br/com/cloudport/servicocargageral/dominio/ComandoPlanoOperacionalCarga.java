package br.com.cloudport.servicocargageral.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "comando_plano_operacional_carga", uniqueConstraints = {
    @UniqueConstraint(name = "uk_comando_plano_command", columnNames = {"plano_id", "command_id"})
})
public class ComandoPlanoOperacionalCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoOperacionalCarga plano;

    @Column(name = "command_id", nullable = false, length = 120)
    private String commandId;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(name = "processado_em", nullable = false)
    private OffsetDateTime processadoEm;

    @PrePersist
    void prePersist() {
        commandId = commandId.trim().toUpperCase();
        processadoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public PlanoOperacionalCarga getPlano() { return plano; }
    public void setPlano(PlanoOperacionalCarga plano) { this.plano = plano; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public OffsetDateTime getProcessadoEm() { return processadoEm; }
}
