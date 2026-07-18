package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAlocacaoCargoLot;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "alocacao_cargo_lot")
public class AlocacaoCargoLot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "reserva_capacidade_id", nullable = false)
    private UUID reservaCapacidadeId;

    @Column(name = "origem", length = 120)
    private String origem;

    @Column(name = "destino", nullable = false, length = 120)
    private String destino;

    @Column(name = "recurso", nullable = false, length = 120)
    private String recurso;

    @Column(name = "prioridade", nullable = false)
    private int prioridade;

    @Column(name = "restricoes", length = 1000)
    private String restricoes;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusAlocacaoCargoLot status = StatusAlocacaoCargoLot.RESERVADA;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "confirmado_em")
    private OffsetDateTime confirmadoEm;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @PrePersist
    void prePersist() {
        criadoEm = OffsetDateTime.now();
    }

    public void confirmar() {
        if (status == StatusAlocacaoCargoLot.CONFIRMADA) return;
        if (status != StatusAlocacaoCargoLot.RESERVADA) {
            throw new IllegalStateException("Allocation não está reservada.");
        }
        status = StatusAlocacaoCargoLot.CONFIRMADA;
        confirmadoEm = OffsetDateTime.now();
    }

    public void cancelar(String motivo) {
        if (status == StatusAlocacaoCargoLot.CONFIRMADA) {
            throw new IllegalStateException("Allocation confirmada não pode ser cancelada.");
        }
        status = StatusAlocacaoCargoLot.CANCELADA;
        motivoCancelamento = motivo;
    }

    public UUID getId() { return id; }
    public UUID getCommandId() { return commandId; }
    public void setCommandId(UUID commandId) { this.commandId = commandId; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public UUID getReservaCapacidadeId() { return reservaCapacidadeId; }
    public void setReservaCapacidadeId(UUID reservaCapacidadeId) { this.reservaCapacidadeId = reservaCapacidadeId; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public int getPrioridade() { return prioridade; }
    public void setPrioridade(int prioridade) { this.prioridade = prioridade; }
    public String getRestricoes() { return restricoes; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public StatusAlocacaoCargoLot getStatus() { return status; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getConfirmadoEm() { return confirmadoEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
}
