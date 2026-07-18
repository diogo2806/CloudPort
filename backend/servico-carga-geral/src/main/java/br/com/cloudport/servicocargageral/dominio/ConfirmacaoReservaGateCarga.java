package br.com.cloudport.servicocargageral.dominio;

import java.math.BigDecimal;
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

@Entity
@Table(name = "confirmacao_reserva_gate_carga")
public class ConfirmacaoReservaGateCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaGateCarga reserva;

    @Column(name = "confirmacao_id", nullable = false, unique = true, length = 120)
    private String confirmacaoId;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "confirmado_em", nullable = false)
    private OffsetDateTime confirmadoEm;

    @PrePersist
    void prePersist() {
        confirmacaoId = confirmacaoId.trim().toUpperCase();
        confirmadoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public ReservaGateCarga getReserva() { return reserva; }
    public void setReserva(ReservaGateCarga reserva) { this.reserva = reserva; }
    public String getConfirmacaoId() { return confirmacaoId; }
    public void setConfirmacaoId(String confirmacaoId) { this.confirmacaoId = confirmacaoId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public OffsetDateTime getConfirmadoEm() { return confirmadoEm; }
}
