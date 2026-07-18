package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusTransload;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "operacao_transload")
public class OperacaoTransload {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @Column(name = "unidade_origem", nullable = false, length = 80)
    private String unidadeOrigem;

    @Column(name = "unidade_destino", nullable = false, length = 80)
    private String unidadeDestino;

    @Column(name = "reserva_origem_id", nullable = false)
    private UUID reservaOrigemId;

    @Column(name = "reserva_destino_id", nullable = false)
    private UUID reservaDestinoId;

    @Column(name = "lacre_origem", length = 80)
    private String lacreOrigem;

    @Column(name = "lacre_destino", length = 80)
    private String lacreDestino;

    @Column(name = "divergencia", length = 1000)
    private String divergencia;

    @Column(name = "codigo_avaria", length = 80)
    private String codigoAvaria;

    @Column(name = "descricao_avaria", length = 1000)
    private String descricaoAvaria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusTransload status = StatusTransload.CONCLUIDO;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "item_operacao_transload", joinColumns = @JoinColumn(name = "operacao_id"))
    @OrderColumn(name = "ordem")
    private List<ItemTransload> itens = new ArrayList<>();

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "executado_em", nullable = false)
    private OffsetDateTime executadoEm;

    @PrePersist
    void prePersist() {
        executadoEm = OffsetDateTime.now();
        unidadeOrigem = normalizar(unidadeOrigem);
        unidadeDestino = normalizar(unidadeDestino);
    }

    public void adicionarItem(
            UUID loteOrigemId,
            UUID loteDestinoId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg) {
        itens.add(new ItemTransload(loteOrigemId, loteDestinoId, quantidade, volumeM3, pesoKg));
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public UUID getCommandId() { return commandId; }
    public void setCommandId(UUID commandId) { this.commandId = commandId; }
    public String getUnidadeOrigem() { return unidadeOrigem; }
    public void setUnidadeOrigem(String unidadeOrigem) { this.unidadeOrigem = unidadeOrigem; }
    public String getUnidadeDestino() { return unidadeDestino; }
    public void setUnidadeDestino(String unidadeDestino) { this.unidadeDestino = unidadeDestino; }
    public UUID getReservaOrigemId() { return reservaOrigemId; }
    public void setReservaOrigemId(UUID reservaOrigemId) { this.reservaOrigemId = reservaOrigemId; }
    public UUID getReservaDestinoId() { return reservaDestinoId; }
    public void setReservaDestinoId(UUID reservaDestinoId) { this.reservaDestinoId = reservaDestinoId; }
    public String getLacreOrigem() { return lacreOrigem; }
    public void setLacreOrigem(String lacreOrigem) { this.lacreOrigem = lacreOrigem; }
    public String getLacreDestino() { return lacreDestino; }
    public void setLacreDestino(String lacreDestino) { this.lacreDestino = lacreDestino; }
    public String getDivergencia() { return divergencia; }
    public void setDivergencia(String divergencia) { this.divergencia = divergencia; }
    public String getCodigoAvaria() { return codigoAvaria; }
    public void setCodigoAvaria(String codigoAvaria) { this.codigoAvaria = codigoAvaria; }
    public String getDescricaoAvaria() { return descricaoAvaria; }
    public void setDescricaoAvaria(String descricaoAvaria) { this.descricaoAvaria = descricaoAvaria; }
    public StatusTransload getStatus() { return status; }
    public List<ItemTransload> getItens() { return Collections.unmodifiableList(itens); }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public OffsetDateTime getExecutadoEm() { return executadoEm; }

    @Embeddable
    public static class ItemTransload {

        @Column(name = "lote_origem_id", nullable = false)
        private UUID loteOrigemId;

        @Column(name = "lote_destino_id", nullable = false)
        private UUID loteDestinoId;

        @Column(name = "quantidade", nullable = false, precision = 19, scale = 3)
        private BigDecimal quantidade;

        @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
        private BigDecimal volumeM3;

        @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
        private BigDecimal pesoKg;

        protected ItemTransload() {
        }

        public ItemTransload(UUID loteOrigemId, UUID loteDestinoId, BigDecimal quantidade,
                BigDecimal volumeM3, BigDecimal pesoKg) {
            this.loteOrigemId = loteOrigemId;
            this.loteDestinoId = loteDestinoId;
            this.quantidade = quantidade;
            this.volumeM3 = volumeM3;
            this.pesoKg = pesoKg;
        }

        public UUID getLoteOrigemId() { return loteOrigemId; }
        public UUID getLoteDestinoId() { return loteDestinoId; }
        public BigDecimal getQuantidade() { return quantidade; }
        public BigDecimal getVolumeM3() { return volumeM3; }
        public BigDecimal getPesoKg() { return pesoKg; }
    }
}
