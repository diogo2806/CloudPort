package br.com.cloudport.servicoyard.inventario.modelo;

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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "saldo_posicao_cargo_lot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saldo_posicao_cargo_lot",
                columnNames = {"capacidade_id", "lote_id"}))
public class SaldoPosicaoCargoLot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capacidade_id", nullable = false)
    private CapacidadePosicaoCargoLot capacidade;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3 = BigDecimal.ZERO;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg = BigDecimal.ZERO;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }

    public void creditar(BigDecimal quantidade, BigDecimal volumeM3, BigDecimal pesoKg) {
        this.quantidade = this.quantidade.add(valor(quantidade));
        this.volumeM3 = this.volumeM3.add(valor(volumeM3));
        this.pesoKg = this.pesoKg.add(valor(pesoKg));
    }

    public void debitar(BigDecimal quantidade, BigDecimal volumeM3, BigDecimal pesoKg) {
        BigDecimal quantidadeDebito = valor(quantidade);
        BigDecimal volumeDebito = valor(volumeM3);
        BigDecimal pesoDebito = valor(pesoKg);
        if (quantidadeDebito.compareTo(this.quantidade) > 0
                || volumeDebito.compareTo(this.volumeM3) > 0
                || pesoDebito.compareTo(this.pesoKg) > 0) {
            throw new IllegalStateException("Saldo insuficiente do cargo lot na posição de origem.");
        }
        this.quantidade = this.quantidade.subtract(quantidadeDebito);
        this.volumeM3 = this.volumeM3.subtract(volumeDebito);
        this.pesoKg = this.pesoKg.subtract(pesoDebito);
    }

    public boolean estaZerado() {
        return quantidade.signum() == 0 && volumeM3.signum() == 0 && pesoKg.signum() == 0;
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public UUID getId() { return id; }
    public CapacidadePosicaoCargoLot getCapacidade() { return capacidade; }
    public void setCapacidade(CapacidadePosicaoCargoLot capacidade) { this.capacidade = capacidade; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public long getVersao() { return versao; }
}
