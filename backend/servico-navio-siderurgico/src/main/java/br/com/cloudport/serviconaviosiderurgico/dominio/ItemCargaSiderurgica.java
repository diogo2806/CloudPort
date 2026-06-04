package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "item_carga_siderurgica")
public class ItemCargaSiderurgica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id")
    private OperacaoSiderurgica operacao;

    @Column(name = "codigo_lote", nullable = false, length = 60)
    private String codigoLote;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false, length = 40)
    private TipoCargaSiderurgica tipoCarga;

    @Column(nullable = false, length = 120)
    private String produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "peso_unitario_toneladas", precision = 10, scale = 3)
    private BigDecimal pesoUnitarioToneladas;

    @Column(name = "peso_total_toneladas", nullable = false, precision = 12, scale = 3)
    private BigDecimal pesoTotalToneladas;

    private Integer porao;

    @Column(name = "posicao_bordo", length = 40)
    private String posicaoBordo;

    @Column(name = "origem_patio", length = 80)
    private String origemPatio;

    @Column(name = "destino_patio", length = 80)
    private String destinoPatio;

    @Column(name = "sequencia_operacional")
    private Integer sequenciaOperacional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusItemCarga status = StatusItemCarga.PLANEJADO;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public OperacaoSiderurgica getOperacao() { return operacao; }
    public void setOperacao(OperacaoSiderurgica operacao) { this.operacao = operacao; }
    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }
    public TipoCargaSiderurgica getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(TipoCargaSiderurgica tipoCarga) { this.tipoCarga = tipoCarga; }
    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPesoUnitarioToneladas() { return pesoUnitarioToneladas; }
    public void setPesoUnitarioToneladas(BigDecimal pesoUnitarioToneladas) { this.pesoUnitarioToneladas = pesoUnitarioToneladas; }
    public BigDecimal getPesoTotalToneladas() { return pesoTotalToneladas; }
    public void setPesoTotalToneladas(BigDecimal pesoTotalToneladas) { this.pesoTotalToneladas = pesoTotalToneladas; }
    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public String getPosicaoBordo() { return posicaoBordo; }
    public void setPosicaoBordo(String posicaoBordo) { this.posicaoBordo = posicaoBordo; }
    public String getOrigemPatio() { return origemPatio; }
    public void setOrigemPatio(String origemPatio) { this.origemPatio = origemPatio; }
    public String getDestinoPatio() { return destinoPatio; }
    public void setDestinoPatio(String destinoPatio) { this.destinoPatio = destinoPatio; }
    public Integer getSequenciaOperacional() { return sequenciaOperacional; }
    public void setSequenciaOperacional(Integer sequenciaOperacional) { this.sequenciaOperacional = sequenciaOperacional; }
    public StatusItemCarga getStatus() { return status; }
    public void setStatus(StatusItemCarga status) { this.status = status; }
}
