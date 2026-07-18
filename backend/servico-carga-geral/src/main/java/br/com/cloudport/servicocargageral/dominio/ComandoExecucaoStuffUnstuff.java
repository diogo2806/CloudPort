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
@Table(
        name = "comando_execucao_stuff_unstuff",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comando_execucao_stuff_unstuff",
                columnNames = {"operacao_id", "command_id"}))
public class ComandoExecucaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id", nullable = false)
    private OperacaoStuffUnstuff operacao;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "aplicado_em", nullable = false)
    private OffsetDateTime aplicadoEm;

    @PrePersist
    void prePersist() {
        aplicadoEm = OffsetDateTime.now();
    }

    public boolean possuiPayload(String hash) {
        return payloadHash != null && payloadHash.equals(hash);
    }

    public UUID getId() {
        return id;
    }

    public OperacaoStuffUnstuff getOperacao() {
        return operacao;
    }

    public void setOperacao(OperacaoStuffUnstuff operacao) {
        this.operacao = operacao;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public OffsetDateTime getAplicadoEm() {
        return aplicadoEm;
    }
}
