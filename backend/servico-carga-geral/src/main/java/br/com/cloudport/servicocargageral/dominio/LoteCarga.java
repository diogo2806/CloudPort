package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "lote_carga")
public class LoteCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemConhecimentoCarga item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_pai_id")
    private LoteCarga lotePai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NaturezaCarga natureza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusLoteCarga status = StatusLoteCarga.PROGRAMADO;

    @Column(name = "quantidade_prevista", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadePrevista;

    @Column(name = "volume_previsto_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumePrevistoM3;

    @Column(name = "peso_previsto_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoPrevistoKg;

    @Column(name = "quantidade_saldo", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeSaldo = BigDecimal.ZERO;

    @Column(name = "volume_saldo_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeSaldoM3 = BigDecimal.ZERO;

    @Column(name = "peso_saldo_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoSaldoKg = BigDecimal.ZERO;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Column(name = "marcas_embalagem", length = 300)
    private String marcasEmbalagem;

    @Column(name = "armazem_id", length = 80)
    private String armazemId;

    @Column(name = "posicao_armazenagem", length = 120)
    private String posicaoArmazenagem;

    @Column(name = "veiculo_id", length = 80)
    private String veiculoId;

    @Column(name = "visita_navio_id", length = 80)
    private String visitaNavioId;

    @Column(name = "cliente_id", length = 80)
    private String clienteId;

    @Column(name = "codigo_avaria", length = 80)
    private String codigoAvaria;

    @Column(name = "descricao_avaria", length = 1000)
    private String descricaoAvaria;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm DESC")
    private List<MovimentacaoCarga> movimentacoes = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        codigo = normalizar(codigo);
        unidadeMedida = normalizar(unidadeMedida);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        codigo = normalizar(codigo);
        unidadeMedida = normalizar(unidadeMedida);
    }

    public void adicionarSaldo(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        quantidadeSaldo = quantidadeSaldo.add(valor(quantidade));
        volumeSaldoM3 = volumeSaldoM3.add(valor(volume));
        pesoSaldoKg = pesoSaldoKg.add(valor(peso));
        status = StatusLoteCarga.NO_TERMINAL;
    }

    public void retirarSaldo(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        BigDecimal novaQuantidade = quantidadeSaldo.subtract(valor(quantidade));
        BigDecimal novoVolume = volumeSaldoM3.subtract(valor(volume));
        BigDecimal novoPeso = pesoSaldoKg.subtract(valor(peso));
        if (novaQuantidade.signum() < 0 || novoVolume.signum() < 0 || novoPeso.signum() < 0) {
            throw new IllegalStateException("Movimentação excede o saldo disponível do lote.");
        }
        quantidadeSaldo = novaQuantidade;
        volumeSaldoM3 = novoVolume;
        pesoSaldoKg = novoPeso;
        status = novaQuantidade.signum() == 0 && novoVolume.signum() == 0 && novoPeso.signum() == 0
                ? StatusLoteCarga.CONCLUIDO
                : StatusLoteCarga.PARCIALMENTE_CARREGADO;
    }

    public void registrarMovimentacao(MovimentacaoCarga movimentacao) {
        movimentacao.setLote(this);
        movimentacoes.add(movimentacao);
    }

    public void atualizarLocalizacao(String armazem, String posicao, String veiculo, String visitaNavio, String cliente) {
        if (armazem != null) armazemId = armazem;
        if (posicao != null) posicaoArmazenagem = posicao;
        if (veiculo != null) veiculoId = veiculo;
        if (visitaNavio != null) visitaNavioId = visitaNavio;
        if (cliente != null) clienteId = cliente;
    }

    private BigDecimal valor(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
    private String normalizar(String valor) { return valor == null ? null : valor.trim().toUpperCase(); }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public ItemConhecimentoCarga getItem() { return item; }
    public void setItem(ItemConhecimentoCarga item) { this.item = item; }
    public LoteCarga getLotePai() { return lotePai; }
    public void setLotePai(LoteCarga lotePai) { this.lotePai = lotePai; }
    public NaturezaCarga getNatureza() { return natureza; }
    public void setNatureza(NaturezaCarga natureza) { this.natureza = natureza; }
    public StatusLoteCarga getStatus() { return status; }
    public void setStatus(StatusLoteCarga status) { this.status = status; }
    public BigDecimal getQuantidadePrevista() { return quantidadePrevista; }
    public void setQuantidadePrevista(BigDecimal quantidadePrevista) { this.quantidadePrevista = quantidadePrevista; }
    public BigDecimal getVolumePrevistoM3() { return volumePrevistoM3; }
    public void setVolumePrevistoM3(BigDecimal volumePrevistoM3) { this.volumePrevistoM3 = volumePrevistoM3; }
    public BigDecimal getPesoPrevistoKg() { return pesoPrevistoKg; }
    public void setPesoPrevistoKg(BigDecimal pesoPrevistoKg) { this.pesoPrevistoKg = pesoPrevistoKg; }
    public BigDecimal getQuantidadeSaldo() { return quantidadeSaldo; }
    public BigDecimal getVolumeSaldoM3() { return volumeSaldoM3; }
    public BigDecimal getPesoSaldoKg() { return pesoSaldoKg; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public String getMarcasEmbalagem() { return marcasEmbalagem; }
    public void setMarcasEmbalagem(String marcasEmbalagem) { this.marcasEmbalagem = marcasEmbalagem; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getPosicaoArmazenagem() { return posicaoArmazenagem; }
    public void setPosicaoArmazenagem(String posicaoArmazenagem) { this.posicaoArmazenagem = posicaoArmazenagem; }
    public String getVeiculoId() { return veiculoId; }
    public void setVeiculoId(String veiculoId) { this.veiculoId = veiculoId; }
    public String getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(String visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getCodigoAvaria() { return codigoAvaria; }
    public void setCodigoAvaria(String codigoAvaria) { this.codigoAvaria = codigoAvaria; }
    public String getDescricaoAvaria() { return descricaoAvaria; }
    public void setDescricaoAvaria(String descricaoAvaria) { this.descricaoAvaria = descricaoAvaria; }
    public List<MovimentacaoCarga> getMovimentacoes() { return Collections.unmodifiableList(movimentacoes); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
