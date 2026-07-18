package br.com.cloudport.servicocargageral.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "item_conhecimento_carga", uniqueConstraints = {
        @UniqueConstraint(name = "uk_item_conhecimento_sequencia", columnNames = {"conhecimento_id", "sequencia"})
})
public class ItemConhecimentoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conhecimento_id", nullable = false)
    private ConhecimentoCarga conhecimento;

    @Column(nullable = false)
    private int sequencia;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(name = "commodity_codigo", nullable = false, length = 80)
    private String commodityCodigo;

    @Column(name = "tipo_produto_codigo", nullable = false, length = 80)
    private String tipoProdutoCodigo;

    @Column(name = "tipo_embalagem_codigo", nullable = false, length = 80)
    private String tipoEmbalagemCodigo;

    @Column(name = "quantidade_manifestada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeManifestada;

    @Column(name = "volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "codigo_armazenagem", length = 80)
    private String codigoArmazenagem;

    @Column(name = "codigo_manuseio", length = 80)
    private String codigoManuseio;

    @Column(name = "mercadoria_perigosa", nullable = false)
    private boolean mercadoriaPerigosa;

    @Column(name = "numero_un", length = 20)
    private String numeroUn;

    @Column(name = "classe_imdg", length = 20)
    private String classeImdg;

    @Column(name = "temperatura_minima", precision = 8, scale = 2)
    private BigDecimal temperaturaMinima;

    @Column(name = "temperatura_maxima", precision = 8, scale = 2)
    private BigDecimal temperaturaMaxima;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("codigo ASC")
    private List<LoteCarga> lotes = new ArrayList<>();

    public void adicionarLote(LoteCarga lote) {
        lote.setItem(this);
        lotes.add(lote);
    }

    public UUID getId() { return id; }
    public ConhecimentoCarga getConhecimento() { return conhecimento; }
    public void setConhecimento(ConhecimentoCarga conhecimento) { this.conhecimento = conhecimento; }
    public int getSequencia() { return sequencia; }
    public void setSequencia(int sequencia) { this.sequencia = sequencia; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCommodityCodigo() { return commodityCodigo; }
    public void setCommodityCodigo(String commodityCodigo) { this.commodityCodigo = commodityCodigo; }
    public String getTipoProdutoCodigo() { return tipoProdutoCodigo; }
    public void setTipoProdutoCodigo(String tipoProdutoCodigo) { this.tipoProdutoCodigo = tipoProdutoCodigo; }
    public String getTipoEmbalagemCodigo() { return tipoEmbalagemCodigo; }
    public void setTipoEmbalagemCodigo(String tipoEmbalagemCodigo) { this.tipoEmbalagemCodigo = tipoEmbalagemCodigo; }
    public BigDecimal getQuantidadeManifestada() { return quantidadeManifestada; }
    public void setQuantidadeManifestada(BigDecimal quantidadeManifestada) { this.quantidadeManifestada = quantidadeManifestada; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public String getCodigoArmazenagem() { return codigoArmazenagem; }
    public void setCodigoArmazenagem(String codigoArmazenagem) { this.codigoArmazenagem = codigoArmazenagem; }
    public String getCodigoManuseio() { return codigoManuseio; }
    public void setCodigoManuseio(String codigoManuseio) { this.codigoManuseio = codigoManuseio; }
    public boolean isMercadoriaPerigosa() { return mercadoriaPerigosa; }
    public void setMercadoriaPerigosa(boolean mercadoriaPerigosa) { this.mercadoriaPerigosa = mercadoriaPerigosa; }
    public String getNumeroUn() { return numeroUn; }
    public void setNumeroUn(String numeroUn) { this.numeroUn = numeroUn; }
    public String getClasseImdg() { return classeImdg; }
    public void setClasseImdg(String classeImdg) { this.classeImdg = classeImdg; }
    public BigDecimal getTemperaturaMinima() { return temperaturaMinima; }
    public void setTemperaturaMinima(BigDecimal temperaturaMinima) { this.temperaturaMinima = temperaturaMinima; }
    public BigDecimal getTemperaturaMaxima() { return temperaturaMaxima; }
    public void setTemperaturaMaxima(BigDecimal temperaturaMaxima) { this.temperaturaMaxima = temperaturaMaxima; }
    public List<LoteCarga> getLotes() { return Collections.unmodifiableList(lotes); }
}
