package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.EstagioGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusReservaGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoMovimentoGateCarga;
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
@Table(name = "reserva_gate_carga")
public class ReservaGateCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id_reserva", nullable = false, unique = true)
    private UUID commandIdReserva;

    @Column(name = "command_id_confirmacao", unique = true)
    private UUID commandIdConfirmacao;

    @Column(name = "command_id_compensacao", unique = true)
    private UUID commandIdCompensacao;

    @Column(name = "agendamento_codigo", nullable = false, length = 80)
    private String agendamentoCodigo;

    @Column(name = "bl_numero", nullable = false, length = 100)
    private String blNumero;

    @Column(name = "delivery_order", nullable = false, length = 100)
    private String deliveryOrder;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 20)
    private TipoMovimentoGateCarga tipoMovimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estagio_confirmacao", nullable = false, length = 20)
    private EstagioGateCarga estagioConfirmacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReservaGateCarga status = StatusReservaGateCarga.RESERVADA;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "usuario_reserva", nullable = false, length = 120)
    private String usuarioReserva;

    @Column(name = "reservado_em", nullable = false)
    private OffsetDateTime reservadoEm;

    @Column(name = "usuario_confirmacao", length = 120)
    private String usuarioConfirmacao;

    @Column(name = "confirmado_em")
    private OffsetDateTime confirmadoEm;

    @Column(name = "motivo_compensacao", length = 1000)
    private String motivoCompensacao;

    @Column(name = "compensado_em")
    private OffsetDateTime compensadoEm;

    @PrePersist
    void prePersist() {
        reservadoEm = OffsetDateTime.now();
    }

    public void confirmar(UUID commandId, String usuario, EstagioGateCarga estagio) {
        if (status == StatusReservaGateCarga.CONFIRMADA && commandId.equals(commandIdConfirmacao)) {
            return;
        }
        if (status != StatusReservaGateCarga.RESERVADA) {
            throw new IllegalStateException("Reserva do Gate não está disponível para confirmação.");
        }
        if (estagio != estagioConfirmacao) {
            throw new IllegalStateException("Estágio físico incompatível com a reserva da carga.");
        }
        commandIdConfirmacao = commandId;
        usuarioConfirmacao = usuario;
        confirmadoEm = OffsetDateTime.now();
        status = StatusReservaGateCarga.CONFIRMADA;
    }

    public void compensar(UUID commandId, String motivo) {
        if (status == StatusReservaGateCarga.COMPENSADA && commandId.equals(commandIdCompensacao)) {
            return;
        }
        if (status == StatusReservaGateCarga.COMPENSADA) {
            throw new IllegalStateException("Reserva já foi compensada por outro comando.");
        }
        commandIdCompensacao = commandId;
        motivoCompensacao = motivo;
        compensadoEm = OffsetDateTime.now();
        status = StatusReservaGateCarga.COMPENSADA;
    }

    public UUID getId() { return id; }
    public UUID getCommandIdReserva() { return commandIdReserva; }
    public void setCommandIdReserva(UUID commandIdReserva) { this.commandIdReserva = commandIdReserva; }
    public UUID getCommandIdConfirmacao() { return commandIdConfirmacao; }
    public UUID getCommandIdCompensacao() { return commandIdCompensacao; }
    public String getAgendamentoCodigo() { return agendamentoCodigo; }
    public void setAgendamentoCodigo(String agendamentoCodigo) { this.agendamentoCodigo = agendamentoCodigo; }
    public String getBlNumero() { return blNumero; }
    public void setBlNumero(String blNumero) { this.blNumero = blNumero; }
    public String getDeliveryOrder() { return deliveryOrder; }
    public void setDeliveryOrder(String deliveryOrder) { this.deliveryOrder = deliveryOrder; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public TipoMovimentoGateCarga getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimentoGateCarga tipoMovimento) { this.tipoMovimento = tipoMovimento; }
    public EstagioGateCarga getEstagioConfirmacao() { return estagioConfirmacao; }
    public void setEstagioConfirmacao(EstagioGateCarga estagioConfirmacao) { this.estagioConfirmacao = estagioConfirmacao; }
    public StatusReservaGateCarga getStatus() { return status; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public String getUsuarioReserva() { return usuarioReserva; }
    public void setUsuarioReserva(String usuarioReserva) { this.usuarioReserva = usuarioReserva; }
    public OffsetDateTime getReservadoEm() { return reservadoEm; }
    public String getUsuarioConfirmacao() { return usuarioConfirmacao; }
    public OffsetDateTime getConfirmadoEm() { return confirmadoEm; }
    public String getMotivoCompensacao() { return motivoCompensacao; }
    public OffsetDateTime getCompensadoEm() { return compensadoEm; }
}
