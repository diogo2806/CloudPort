package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(
        name = "crane_sequence_audit",
        indexes = @Index(name = "idx_crane_sequence_audit_movement", columnList = "movement_id, occurred_at"))
public class AuditoriaSequenciaGuindaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_id", nullable = false, length = 120)
    private String movementId;

    @Column(nullable = false, length = 40)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 20)
    private StatusSequenciaGuindaste statusBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", length = 20)
    private StatusSequenciaGuindaste statusAfter;

    @Column(name = "operator_id", nullable = false, length = 120)
    private String operatorId;

    @Column(length = 1000)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getMovementId() { return movementId; }
    public void setMovementId(String movementId) { this.movementId = movementId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public StatusSequenciaGuindaste getStatusBefore() { return statusBefore; }
    public void setStatusBefore(StatusSequenciaGuindaste statusBefore) { this.statusBefore = statusBefore; }
    public StatusSequenciaGuindaste getStatusAfter() { return statusAfter; }
    public void setStatusAfter(StatusSequenciaGuindaste statusAfter) { this.statusAfter = statusAfter; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
