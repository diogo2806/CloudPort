package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.ModalTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusPlanoTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoOperacaoTransporteCargo;
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
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "plano_transporte_cargo_lot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plano_transporte_cargo",
                columnNames = {"modal", "visita_id", "lote_id", "sequencia"}))
public class PlanoTransporteCargoLot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id_planejamento", nullable = false, unique = true)
    private UUID commandIdPlanejamento;

    @Column(name = "command_id_execucao", unique = true)
    private UUID commandIdExecucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "modal", nullable = false, length = 20)
    private ModalTransporteCargo modal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 20)
    private TipoOperacaoTransporteCargo tipoOperacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPlanoTransporteCargo status = StatusPlanoTransporteCargo.PLANEJADO;

    @Column(name = "visita_id", nullable = false, length = 80)
    private String visitaId;

    @Column(name = "bl_numero", nullable = false, length = 100)
    private String blNumero;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "compartimento", nullable = false, length = 120)
    private String compartimento;

    @Column(name = "posicao", length = 120)
    private String posicao;

    @Column(name = "sequencia", nullable = false)
    private int sequencia;

    @Column(name = "equipamento", nullable = false, length = 120)
    private String equipamento;

    @Column(name = "custodia", length = 120)
    private String custodia;

    @Column(name = "restricoes", length = 1000)
    private String restricoes;

    @Column(name = "capacidade_peso_kg", precision = 19, scale = 3)
    private BigDecimal capacidadePesoKg;

    @Column(name = "quantidade_planejada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadePlanejada;

    @Column(name = "volume_planejado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumePlanejadoM3;

    @Column(name = "peso_planejado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoPlanejadoKg;

    @Column(name = "quantidade_realizada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeRealizada = BigDecimal.ZERO;

    @Column(name = "volume_realizado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeRealizadoM3 = BigDecimal.ZERO;

    @Column(name = "peso_realizado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoRealizadoKg = BigDecimal.ZERO;

    @Column(name = "usuario_planejamento", nullable = false, length = 120)
    private String usuarioPlanejamento;

    @Column(name = "planejado_em", nullable = false)
    private OffsetDateTime planejadoEm;

    @Column(name = "usuario_execucao", length = 120)
    private String usuarioExecucao;

    @Column(name = "executado_em")
    private OffsetDateTime executadoEm;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @PrePersist
    void prePersist() {
        planejadoEm = OffsetDateTime.now();
    }

    public void executar(UUID commandId, BigDecimal quantidade, BigDecimal volumeM3, BigDecimal pesoKg, String usuario) {
        if (status == StatusPlanoTransporteCargo.CONCLUIDO && commandId.equals(commandIdExecucao)) return;
        if (status == StatusPlanoTransporteCargo.CANCELADO) {
            throw new IllegalStateException("Plano de transporte cancelado.");
        }
        if (quantidade.compareTo(quantidadePlanejada.subtract(quantidadeRealizada)) > 0
                || volumeM3.compareTo(volumePlanejadoM3.subtract(volumeRealizadoM3)) > 0
                || pesoKg.compareTo(pesoPlanejadoKg.subtract(pesoRealizadoKg)) > 0) {
            throw new IllegalStateException("Execução excede o saldo planejado do transporte.");
        }
        commandIdExecucao = commandId;
        quantidadeRealizada = quantidadeRealizada.add(quantidade);
        volumeRealizadoM3 = volumeRealizadoM3.add(volumeM3);
        pesoRealizadoKg = pesoRealizadoKg.add(pesoKg);
        usuarioExecucao = usuario;
        executadoEm = OffsetDateTime.now();
        status = quantidadeRealizada.compareTo(quantidadePlanejada) == 0
                && volumeRealizadoM3.compareTo(volumePlanejadoM3) == 0
                && pesoRealizadoKg.compareTo(pesoPlanejadoKg) == 0
                ? StatusPlanoTransporteCargo.CONCLUIDO
                : StatusPlanoTransporteCargo.EM_EXECUCAO;
    }

    public void cancelar(String motivo) {
        if (quantidadeRealizada.signum() > 0 || volumeRealizadoM3.signum() > 0 || pesoRealizadoKg.signum() > 0) {
            throw new IllegalStateException("Plano com execução física não pode ser cancelado.");
        }
        status = StatusPlanoTransporteCargo.CANCELADO;
        motivoCancelamento = motivo;
    }

    public UUID getId() { return id; }
    public UUID getCommandIdPlanejamento() { return commandIdPlanejamento; }
    public void setCommandIdPlanejamento(UUID commandIdPlanejamento) { this.commandIdPlanejamento = commandIdPlanejamento; }
    public UUID getCommandIdExecucao() { return commandIdExecucao; }
    public ModalTransporteCargo getModal() { return modal; }
    public void setModal(ModalTransporteCargo modal) { this.modal = modal; }
    public TipoOperacaoTransporteCargo getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoTransporteCargo tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public StatusPlanoTransporteCargo getStatus() { return status; }
    public String getVisitaId() { return visitaId; }
    public void setVisitaId(String visitaId) { this.visitaId = visitaId; }
    public String getBlNumero() { return blNumero; }
    public void setBlNumero(String blNumero) { this.blNumero = blNumero; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public String getCompartimento() { return compartimento; }
    public void setCompartimento(String compartimento) { this.compartimento = compartimento; }
    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = posicao; }
    public int getSequencia() { return sequencia; }
    public void setSequencia(int sequencia) { this.sequencia = sequencia; }
    public String getEquipamento() { return equipamento; }
    public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
    public String getCustodia() { return custodia; }
    public void setCustodia(String custodia) { this.custodia = custodia; }
    public String getRestricoes() { return restricoes; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public BigDecimal getCapacidadePesoKg() { return capacidadePesoKg; }
    public void setCapacidadePesoKg(BigDecimal capacidadePesoKg) { this.capacidadePesoKg = capacidadePesoKg; }
    public BigDecimal getQuantidadePlanejada() { return quantidadePlanejada; }
    public void setQuantidadePlanejada(BigDecimal quantidadePlanejada) { this.quantidadePlanejada = quantidadePlanejada; }
    public BigDecimal getVolumePlanejadoM3() { return volumePlanejadoM3; }
    public void setVolumePlanejadoM3(BigDecimal volumePlanejadoM3) { this.volumePlanejadoM3 = volumePlanejadoM3; }
    public BigDecimal getPesoPlanejadoKg() { return pesoPlanejadoKg; }
    public void setPesoPlanejadoKg(BigDecimal pesoPlanejadoKg) { this.pesoPlanejadoKg = pesoPlanejadoKg; }
    public BigDecimal getQuantidadeRealizada() { return quantidadeRealizada; }
    public BigDecimal getVolumeRealizadoM3() { return volumeRealizadoM3; }
    public BigDecimal getPesoRealizadoKg() { return pesoRealizadoKg; }
    public String getUsuarioPlanejamento() { return usuarioPlanejamento; }
    public void setUsuarioPlanejamento(String usuarioPlanejamento) { this.usuarioPlanejamento = usuarioPlanejamento; }
    public OffsetDateTime getPlanejadoEm() { return planejadoEm; }
    public String getUsuarioExecucao() { return usuarioExecucao; }
    public OffsetDateTime getExecutadoEm() { return executadoEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
}
