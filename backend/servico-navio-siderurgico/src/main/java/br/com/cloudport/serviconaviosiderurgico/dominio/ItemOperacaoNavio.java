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
@Table(name = "item_operacao_navio")
public class ItemOperacaoNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_navio_id", nullable = false)
    private VisitaNavio visitaNavio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 20)
    private TipoMovimentoNavio tipoMovimento;

    @Column(name = "codigo_lote", nullable = false, length = 80)
    private String codigoLote;

    @Column(nullable = false, length = 120)
    private String produto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false, length = 40)
    private TipoCargaSiderurgica tipoCarga;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "peso_unitario_toneladas", precision = 12, scale = 3)
    private BigDecimal pesoUnitarioToneladas;

    @Column(name = "peso_total_toneladas", nullable = false, precision = 14, scale = 3)
    private BigDecimal pesoTotalToneladas;

    @Column(name = "porao_planejado")
    private Integer poraoPlanejado;

    @Column(name = "porao_real")
    private Integer poraoReal;

    @Column(name = "posicao_planejada", length = 80)
    private String posicaoPlanejada;

    @Column(name = "posicao_real", length = 80)
    private String posicaoReal;

    @Column(name = "origem_patio", length = 80)
    private String origemPatio;

    @Column(name = "destino_patio", length = 80)
    private String destinoPatio;

    @Column(name = "sequencia_operacional")
    private Integer sequenciaOperacional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusItemCarga status = StatusItemCarga.PLANEJADO;

    @Column(name = "motivo_bloqueio", length = 500)
    private String motivoBloqueio;

    @Column(length = 1000)
    private String observacoes;

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
    public VisitaNavio getVisitaNavio() { return visitaNavio; }
    public void setVisitaNavio(VisitaNavio visitaNavio) { this.visitaNavio = visitaNavio; }
    public TipoMovimentoNavio getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimentoNavio tipoMovimento) { this.tipoMovimento = tipoMovimento; }
    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }
    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }
    public TipoCargaSiderurgica getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(TipoCargaSiderurgica tipoCarga) { this.tipoCarga = tipoCarga; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPesoUnitarioToneladas() { return pesoUnitarioToneladas; }
    public void setPesoUnitarioToneladas(BigDecimal pesoUnitarioToneladas) { this.pesoUnitarioToneladas = pesoUnitarioToneladas; }
    public BigDecimal getPesoTotalToneladas() { return pesoTotalToneladas; }
    public void setPesoTotalToneladas(BigDecimal pesoTotalToneladas) { this.pesoTotalToneladas = pesoTotalToneladas; }
    public Integer getPoraoPlanejado() { return poraoPlanejado; }
    public void setPoraoPlanejado(Integer poraoPlanejado) { this.poraoPlanejado = poraoPlanejado; }
    public Integer getPoraoReal() { return poraoReal; }
    public void setPoraoReal(Integer poraoReal) { this.poraoReal = poraoReal; }
    public String getPosicaoPlanejada() { return posicaoPlanejada; }
    public void setPosicaoPlanejada(String posicaoPlanejada) { this.posicaoPlanejada = posicaoPlanejada; }
    public String getPosicaoReal() { return posicaoReal; }
    public void setPosicaoReal(String posicaoReal) { this.posicaoReal = posicaoReal; }
    public String getOrigemPatio() { return origemPatio; }
    public void setOrigemPatio(String origemPatio) { this.origemPatio = origemPatio; }
    public String getDestinoPatio() { return destinoPatio; }
    public void setDestinoPatio(String destinoPatio) { this.destinoPatio = destinoPatio; }
    public Integer getSequenciaOperacional() { return sequenciaOperacional; }
    public void setSequenciaOperacional(Integer sequenciaOperacional) { this.sequenciaOperacional = sequenciaOperacional; }
    public StatusItemCarga getStatus() { return status; }
    public void setStatus(StatusItemCarga status) { this.status = status; }
    public String getMotivoBloqueio() { return motivoBloqueio; }
    public void setMotivoBloqueio(String motivoBloqueio) { this.motivoBloqueio = motivoBloqueio; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
