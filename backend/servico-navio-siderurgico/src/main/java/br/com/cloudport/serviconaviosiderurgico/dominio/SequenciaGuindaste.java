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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "crane_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_crane_sequence_movement", columnNames = "movement_id"),
        indexes = {
                @Index(name = "idx_crane_sequence_vessel_visit", columnList = "vessel_visit_id"),
                @Index(name = "idx_crane_sequence_status", columnList = "status"),
                @Index(name = "idx_crane_sequence_planned_start", columnList = "planned_start")
        })
public class SequenciaGuindaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_id", nullable = false, length = 120)
    private String movementId;

    @Column(name = "vessel_visit_id", nullable = false, length = 120)
    private String vesselVisitId;

    @Column(name = "crane_id", nullable = false, length = 80)
    private String craneId;

    @Column(name = "load_unit_id", nullable = false, length = 120)
    private String loadUnitId;

    @Column(name = "planned_start", nullable = false)
    private LocalDateTime plannedStart;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSequenciaGuindaste status = StatusSequenciaGuindaste.PLANNED;

    @Column(name = "operator_id", length = 120)
    private String operatorId;

    @Column(length = 4000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        createdAt = agora;
        updatedAt = agora;
        if (status == null) {
            status = StatusSequenciaGuindaste.PLANNED;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getMovementId() { return movementId; }
    public void setMovementId(String movementId) { this.movementId = movementId; }
    public String getVesselVisitId() { return vesselVisitId; }
    public void setVesselVisitId(String vesselVisitId) { this.vesselVisitId = vesselVisitId; }
    public String getCraneId() { return craneId; }
    public void setCraneId(String craneId) { this.craneId = craneId; }
    public String getLoadUnitId() { return loadUnitId; }
    public void setLoadUnitId(String loadUnitId) { this.loadUnitId = loadUnitId; }
    public LocalDateTime getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDateTime plannedStart) { this.plannedStart = plannedStart; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public StatusSequenciaGuindaste getStatus() { return status; }
    public void setStatus(StatusSequenciaGuindaste status) { this.status = status; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
