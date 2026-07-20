package br.com.cloudport.servicocargageral.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "ordem_liberacao_stuff_unstuff", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ordem_liberacao_operacao", columnNames = "operacao_id"),
    @UniqueConstraint(name = "uk_ordem_liberacao_origem", columnNames = {"tipo_origem", "identificador_origem", "versao_origem"})
})
public class OrdemLiberacaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private long versaoRegistro;

    @Column(name = "operacao_id", nullable = false)
    private UUID operacaoId;

    @Column(name = "tipo_origem", nullable = false, length = 40)
    private String tipoOrigem;

    @Column(name = "identificador_origem", nullable = false, length = 120)
    private String identificadorOrigem;

    @Column(name = "versao_origem", nullable = false)
    private long versaoOrigem;

    @Column(name = "quantidade_autorizada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeAutorizada;

    @Column(name = "quantidade_reservada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeReservada = BigDecimal.ZERO;

    @Column(name = "quantidade_consumida", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeConsumida = BigDecimal.ZERO;

    @Column(name = "vigente_de", nullable = false)
    private OffsetDateTime vigenteDe;

    @Column(name = "vigente_ate", nullable = false)
    private OffsetDateTime vigenteAte;

    @Column(nullable = false)
    private boolean hold;

    @Column(name = "snapshot_origem", nullable = false, length = 4000)
    private String snapshotOrigem;

    @Column(name = "motivo_bloqueio", length = 1000)
    private String motivoBloqueio;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = OffsetDateTime.now();
    }

    public void reservar(BigDecimal quantidade, OffsetDateTime agora) {
        validarVigencia(agora);
        if (hold) {
            bloquear("A origem operacional está em hold.");
        }
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            bloquear("A quantidade reservada deve ser positiva.");
        }
        if (quantidade.compareTo(quantidadeAutorizada) > 0) {
            bloquear("Saldo insuficiente na origem operacional.");
        }
        quantidadeReservada = quantidade;
        motivoBloqueio = null;
    }

    public void validarParaInicio(OffsetDateTime agora) {
        validarVigencia(agora);
        if (hold) {
            bloquear("A origem operacional está em hold.");
        }
        if (quantidadeReservada.compareTo(BigDecimal.ZERO) <= 0) {
            bloquear("A origem operacional não possui saldo reservado.");
        }
    }

    public void consumir(BigDecimal quantidade) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            bloquear("A quantidade consumida deve ser positiva.");
        }
        BigDecimal novoConsumo = quantidadeConsumida.add(quantidade);
        if (novoConsumo.compareTo(quantidadeReservada) > 0) {
            bloquear("A execução excede o saldo reservado da origem operacional.");
        }
        quantidadeConsumida = novoConsumo;
        motivoBloqueio = null;
    }

    public void concluir() {
        if (quantidadeConsumida.compareTo(quantidadeReservada) != 0) {
            bloquear("A ordem comercial só pode ser concluída após consumo integral da reserva.");
        }
        motivoBloqueio = null;
    }

    public void compensarCancelamento() {
        quantidadeConsumida = BigDecimal.ZERO;
        quantidadeReservada = BigDecimal.ZERO;
        motivoBloqueio = null;
    }

    private void validarVigencia(OffsetDateTime agora) {
        if (agora.isBefore(vigenteDe) || agora.isAfter(vigenteAte)) {
            bloquear("A origem operacional está fora da vigência.");
        }
    }

    private void bloquear(String motivo) {
        motivoBloqueio = motivo;
        throw new IllegalStateException(motivo);
    }

    public UUID getId() { return id; }
    public UUID getOperacaoId() { return operacaoId; }
    public void setOperacaoId(UUID operacaoId) { this.operacaoId = operacaoId; }
    public String getTipoOrigem() { return tipoOrigem; }
    public void setTipoOrigem(String tipoOrigem) { this.tipoOrigem = tipoOrigem; }
    public String getIdentificadorOrigem() { return identificadorOrigem; }
    public void setIdentificadorOrigem(String identificadorOrigem) { this.identificadorOrigem = identificadorOrigem; }
    public long getVersaoOrigem() { return versaoOrigem; }
    public void setVersaoOrigem(long versaoOrigem) { this.versaoOrigem = versaoOrigem; }
    public BigDecimal getQuantidadeAutorizada() { return quantidadeAutorizada; }
    public void setQuantidadeAutorizada(BigDecimal quantidadeAutorizada) { this.quantidadeAutorizada = quantidadeAutorizada; }
    public BigDecimal getQuantidadeReservada() { return quantidadeReservada; }
    public BigDecimal getQuantidadeConsumida() { return quantidadeConsumida; }
    public OffsetDateTime getVigenteDe() { return vigenteDe; }
    public void setVigenteDe(OffsetDateTime vigenteDe) { this.vigenteDe = vigenteDe; }
    public OffsetDateTime getVigenteAte() { return vigenteAte; }
    public void setVigenteAte(OffsetDateTime vigenteAte) { this.vigenteAte = vigenteAte; }
    public boolean isHold() { return hold; }
    public void setHold(boolean hold) { this.hold = hold; }
    public String getSnapshotOrigem() { return snapshotOrigem; }
    public void setSnapshotOrigem(String snapshotOrigem) { this.snapshotOrigem = snapshotOrigem; }
    public String getMotivoBloqueio() { return motivoBloqueio; }
}
