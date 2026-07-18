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
@Table(name = "contagem_inventario_carga")
public class ContagemInventarioCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_id", nullable = false)
    private InventarioFisicoCarga inventario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCarga lote;

    @Column(name = "codigo_identificacao", nullable = false, length = 160)
    private String codigoIdentificacao;

    @Column(nullable = false, length = 120)
    private String posicao;

    @Column(name = "numero_contagem", nullable = false)
    private Integer numeroContagem;

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

    @Column(name = "contado_em", nullable = false)
    private OffsetDateTime contadoEm;

    @PrePersist
    void prePersist() {
        contadoEm = OffsetDateTime.now();
        codigoIdentificacao = obrigatorio(codigoIdentificacao).toUpperCase();
        posicao = obrigatorio(posicao).toUpperCase();
        usuario = obrigatorio(usuario);
        quantidade = valor(quantidade);
        volumeM3 = valor(volumeM3);
        pesoKg = valor(pesoKg);
        if (numeroContagem == null || numeroContagem < 1
                || quantidade.signum() < 0 || volumeM3.signum() < 0 || pesoKg.signum() < 0) {
            throw new IllegalStateException("A contagem física possui valores inválidos.");
        }
    }

    private String obrigatorio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Campo obrigatório da contagem não informado.");
        }
        return valor.trim();
    }

    private BigDecimal valor(BigDecimal numero) {
        return numero == null ? BigDecimal.ZERO : numero;
    }

    public UUID getId() { return id; }
    public InventarioFisicoCarga getInventario() { return inventario; }
    public void setInventario(InventarioFisicoCarga inventario) { this.inventario = inventario; }
    public LoteCarga getLote() { return lote; }
    public void setLote(LoteCarga lote) { this.lote = lote; }
    public String getCodigoIdentificacao() { return codigoIdentificacao; }
    public void setCodigoIdentificacao(String codigoIdentificacao) { this.codigoIdentificacao = codigoIdentificacao; }
    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = posicao; }
    public Integer getNumeroContagem() { return numeroContagem; }
    public void setNumeroContagem(Integer numeroContagem) { this.numeroContagem = numeroContagem; }
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
    public OffsetDateTime getContadoEm() { return contadoEm; }
}
