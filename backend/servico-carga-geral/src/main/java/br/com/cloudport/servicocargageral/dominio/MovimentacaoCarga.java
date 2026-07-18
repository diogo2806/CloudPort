package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
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
@Table(name = "movimentacao_carga")
public class MovimentacaoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCarga lote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoMovimentacaoCarga tipo;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "lote_relacionado_id")
    private UUID loteRelacionadoId;

    @Column(name = "origem_tipo", length = 40)
    private String origemTipo;

    @Column(name = "origem_id", length = 120)
    private String origemId;

    @Column(name = "destino_tipo", length = 40)
    private String destinoTipo;

    @Column(name = "destino_id", length = 120)
    private String destinoId;

    @Column(name = "veiculo_id", length = 80)
    private String veiculoId;

    @Column(name = "visita_navio_id", length = 80)
    private String visitaNavioId;

    @Column(name = "armazem_id", length = 80)
    private String armazemId;

    @Column(name = "cliente_id", length = 80)
    private String clienteId;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    @PrePersist
    void prePersist() {
        if (ocorridoEm == null) {
            ocorridoEm = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public LoteCarga getLote() { return lote; }
    public void setLote(LoteCarga lote) { this.lote = lote; }
    public TipoMovimentacaoCarga getTipo() { return tipo; }
    public void setTipo(TipoMovimentacaoCarga tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public UUID getLoteRelacionadoId() { return loteRelacionadoId; }
    public void setLoteRelacionadoId(UUID loteRelacionadoId) { this.loteRelacionadoId = loteRelacionadoId; }
    public String getOrigemTipo() { return origemTipo; }
    public void setOrigemTipo(String origemTipo) { this.origemTipo = origemTipo; }
    public String getOrigemId() { return origemId; }
    public void setOrigemId(String origemId) { this.origemId = origemId; }
    public String getDestinoTipo() { return destinoTipo; }
    public void setDestinoTipo(String destinoTipo) { this.destinoTipo = destinoTipo; }
    public String getDestinoId() { return destinoId; }
    public void setDestinoId(String destinoId) { this.destinoId = destinoId; }
    public String getVeiculoId() { return veiculoId; }
    public void setVeiculoId(String veiculoId) { this.veiculoId = veiculoId; }
    public String getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(String visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(OffsetDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
}
