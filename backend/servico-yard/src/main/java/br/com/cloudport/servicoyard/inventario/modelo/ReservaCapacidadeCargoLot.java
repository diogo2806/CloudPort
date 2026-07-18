package br.com.cloudport.servicoyard.inventario.modelo;

import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.StatusReservaCapacidade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
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
@Table(name = "reserva_capacidade_cargo_lot")
public class ReservaCapacidadeCargoLot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capacidade_id", nullable = false)
    private CapacidadePosicaoCargoLot capacidade;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReservaCapacidade status = StatusReservaCapacidade.RESERVADA;

    @Column(name = "usuario_reserva", nullable = false, length = 120)
    private String usuarioReserva;

    @Column(name = "reservado_em", nullable = false)
    private OffsetDateTime reservadoEm;

    @Column(name = "usuario_finalizacao", length = 120)
    private String usuarioFinalizacao;

    @Column(name = "motivo_finalizacao", length = 1000)
    private String motivoFinalizacao;

    @Column(name = "finalizado_em")
    private OffsetDateTime finalizadoEm;

    @PrePersist
    void prePersist() {
        reservadoEm = OffsetDateTime.now();
    }

    public void confirmar(String usuario, String motivo) {
        if (status == StatusReservaCapacidade.CONFIRMADA) return;
        if (status != StatusReservaCapacidade.RESERVADA) {
            throw new IllegalStateException("Reserva de capacidade não está ativa.");
        }
        status = StatusReservaCapacidade.CONFIRMADA;
        finalizar(usuario, motivo);
    }

    public void cancelar(String usuario, String motivo) {
        if (status == StatusReservaCapacidade.CANCELADA) return;
        if (status == StatusReservaCapacidade.CONFIRMADA) {
            throw new IllegalStateException("Reserva de capacidade já confirmada.");
        }
        status = StatusReservaCapacidade.CANCELADA;
        finalizar(usuario, motivo);
    }

    private void finalizar(String usuario, String motivo) {
        usuarioFinalizacao = usuario;
        motivoFinalizacao = motivo;
        finalizadoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCommandId() { return commandId; }
    public void setCommandId(UUID commandId) { this.commandId = commandId; }
    public CapacidadePosicaoCargoLot getCapacidade() { return capacidade; }
    public void setCapacidade(CapacidadePosicaoCargoLot capacidade) { this.capacidade = capacidade; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public StatusReservaCapacidade getStatus() { return status; }
    public String getUsuarioReserva() { return usuarioReserva; }
    public void setUsuarioReserva(String usuarioReserva) { this.usuarioReserva = usuarioReserva; }
    public OffsetDateTime getReservadoEm() { return reservadoEm; }
    public String getUsuarioFinalizacao() { return usuarioFinalizacao; }
    public String getMotivoFinalizacao() { return motivoFinalizacao; }
    public OffsetDateTime getFinalizadoEm() { return finalizadoEm; }
}
