package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusProgramacaoDocaCarga;
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
import javax.persistence.Table;

@Entity
@Table(name = "reserva_lote_programacao_doca_carga")
public class ReservaLoteProgramacaoDocaCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programacao_id", nullable = false)
    private ProgramacaoDocaCarga programacao;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "lote_codigo", nullable = false, length = 80)
    private String loteCodigo;

    @Column(name = "quantidade_reservada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeReservada;

    @Column(name = "volume_reservado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeReservadoM3;

    @Column(name = "peso_reservado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoReservadoKg;

    @Column(name = "janela_inicio", nullable = false)
    private OffsetDateTime janelaInicio;

    @Column(name = "janela_fim", nullable = false)
    private OffsetDateTime janelaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProgramacaoDocaCarga status;

    void sincronizar(
            ItemOperacaoStuffUnstuff item,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            StatusProgramacaoDocaCarga novoStatus) {
        loteId = item.getLote().getId();
        loteCodigo = item.getLote().getCodigo();
        quantidadeReservada = item.getQuantidadePlanejada();
        volumeReservadoM3 = item.getVolumePlanejadoM3();
        pesoReservadoKg = item.getPesoPlanejadoKg();
        janelaInicio = inicio;
        janelaFim = fim;
        status = novoStatus;
    }

    void alterarStatus(StatusProgramacaoDocaCarga novoStatus) {
        status = novoStatus;
    }

    void setProgramacao(ProgramacaoDocaCarga programacao) {
        this.programacao = programacao;
    }

    public UUID getId() {
        return id;
    }

    public ProgramacaoDocaCarga getProgramacao() {
        return programacao;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public String getLoteCodigo() {
        return loteCodigo;
    }

    public BigDecimal getQuantidadeReservada() {
        return quantidadeReservada;
    }

    public BigDecimal getVolumeReservadoM3() {
        return volumeReservadoM3;
    }

    public BigDecimal getPesoReservadoKg() {
        return pesoReservadoKg;
    }

    public OffsetDateTime getJanelaInicio() {
        return janelaInicio;
    }

    public OffsetDateTime getJanelaFim() {
        return janelaFim;
    }

    public StatusProgramacaoDocaCarga getStatus() {
        return status;
    }
}
