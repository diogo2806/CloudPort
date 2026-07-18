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
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "item_plano_operacional_carga")
public class ItemPlanoOperacionalCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoOperacionalCarga plano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCarga lote;

    @Column(nullable = false)
    private Integer sequencia;

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

    @Column(name = "posicao_planejada", length = 120)
    private String posicaoPlanejada;

    @Column(name = "posicao_origem_real", length = 120)
    private String posicaoOrigemReal;

    @Column(name = "posicao_destino_real", length = 120)
    private String posicaoDestinoReal;

    @Column(name = "area_porao", length = 120)
    private String areaPorao;

    @Column(name = "vagao_id", length = 80)
    private String vagaoId;

    @Column(name = "posicao_vagao", length = 80)
    private String posicaoVagao;

    @Column(name = "capacidade_reservada_kg", precision = 19, scale = 3)
    private BigDecimal capacidadeReservadaKg;

    @Column(length = 1000)
    private String divergencia;

    @Column(name = "codigo_avaria", length = 80)
    private String codigoAvaria;

    @Column(name = "descricao_avaria", length = 1000)
    private String descricaoAvaria;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        inicializar();
        validar();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        inicializar();
        validar();
    }

    public void registrarExecucao(
            BigDecimal quantidade,
            BigDecimal volume,
            BigDecimal peso,
            String origemReal,
            String destinoReal,
            String divergenciaInformada,
            String avaria,
            String descricao) {
        BigDecimal novaQuantidade = quantidadeRealizada.add(valor(quantidade));
        BigDecimal novoVolume = volumeRealizadoM3.add(valor(volume));
        BigDecimal novoPeso = pesoRealizadoKg.add(valor(peso));
        if (novaQuantidade.compareTo(quantidadePlanejada) > 0
                || novoVolume.compareTo(volumePlanejadoM3) > 0
                || novoPeso.compareTo(pesoPlanejadoKg) > 0) {
            throw new IllegalStateException("A execução excede o planejamento do item.");
        }
        quantidadeRealizada = novaQuantidade;
        volumeRealizadoM3 = novoVolume;
        pesoRealizadoKg = novoPeso;
        posicaoOrigemReal = opcional(origemReal);
        posicaoDestinoReal = opcional(destinoReal);
        divergencia = opcionalLivre(divergenciaInformada);
        codigoAvaria = opcional(avaria);
        descricaoAvaria = opcionalLivre(descricao);
    }

    public boolean possuiExecucao() {
        return quantidadeRealizada.signum() > 0 || volumeRealizadoM3.signum() > 0 || pesoRealizadoKg.signum() > 0;
    }

    public boolean estaCompleto() {
        return quantidadeRealizada.compareTo(quantidadePlanejada) == 0
                && volumeRealizadoM3.compareTo(volumePlanejadoM3) == 0
                && pesoRealizadoKg.compareTo(pesoPlanejadoKg) == 0;
    }

    private void inicializar() {
        quantidadePlanejada = valor(quantidadePlanejada);
        volumePlanejadoM3 = valor(volumePlanejadoM3);
        pesoPlanejadoKg = valor(pesoPlanejadoKg);
        quantidadeRealizada = valor(quantidadeRealizada);
        volumeRealizadoM3 = valor(volumeRealizadoM3);
        pesoRealizadoKg = valor(pesoRealizadoKg);
    }

    private void validar() {
        if (quantidadePlanejada.signum() <= 0 || volumePlanejadoM3.signum() < 0 || pesoPlanejadoKg.signum() < 0) {
            throw new IllegalStateException("O planejamento deve possuir quantidade positiva e volume e peso não negativos.");
        }
        if (capacidadeReservadaKg != null && pesoPlanejadoKg.compareTo(capacidadeReservadaKg) > 0) {
            throw new IllegalStateException("O peso planejado excede a capacidade reservada.");
        }
    }

    private BigDecimal valor(BigDecimal numero) {
        return numero == null ? BigDecimal.ZERO : numero;
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase();
    }

    private String opcionalLivre(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public UUID getId() { return id; }
    public PlanoOperacionalCarga getPlano() { return plano; }
    public void setPlano(PlanoOperacionalCarga plano) { this.plano = plano; }
    public LoteCarga getLote() { return lote; }
    public void setLote(LoteCarga lote) { this.lote = lote; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }
    public BigDecimal getQuantidadePlanejada() { return quantidadePlanejada; }
    public void setQuantidadePlanejada(BigDecimal quantidadePlanejada) { this.quantidadePlanejada = quantidadePlanejada; }
    public BigDecimal getVolumePlanejadoM3() { return volumePlanejadoM3; }
    public void setVolumePlanejadoM3(BigDecimal volumePlanejadoM3) { this.volumePlanejadoM3 = volumePlanejadoM3; }
    public BigDecimal getPesoPlanejadoKg() { return pesoPlanejadoKg; }
    public void setPesoPlanejadoKg(BigDecimal pesoPlanejadoKg) { this.pesoPlanejadoKg = pesoPlanejadoKg; }
    public BigDecimal getQuantidadeRealizada() { return quantidadeRealizada; }
    public BigDecimal getVolumeRealizadoM3() { return volumeRealizadoM3; }
    public BigDecimal getPesoRealizadoKg() { return pesoRealizadoKg; }
    public String getPosicaoPlanejada() { return posicaoPlanejada; }
    public void setPosicaoPlanejada(String posicaoPlanejada) { this.posicaoPlanejada = posicaoPlanejada; }
    public String getPosicaoOrigemReal() { return posicaoOrigemReal; }
    public String getPosicaoDestinoReal() { return posicaoDestinoReal; }
    public String getAreaPorao() { return areaPorao; }
    public void setAreaPorao(String areaPorao) { this.areaPorao = areaPorao; }
    public String getVagaoId() { return vagaoId; }
    public void setVagaoId(String vagaoId) { this.vagaoId = vagaoId; }
    public String getPosicaoVagao() { return posicaoVagao; }
    public void setPosicaoVagao(String posicaoVagao) { this.posicaoVagao = posicaoVagao; }
    public BigDecimal getCapacidadeReservadaKg() { return capacidadeReservadaKg; }
    public void setCapacidadeReservadaKg(BigDecimal capacidadeReservadaKg) { this.capacidadeReservadaKg = capacidadeReservadaKg; }
    public String getDivergencia() { return divergencia; }
    public String getCodigoAvaria() { return codigoAvaria; }
    public String getDescricaoAvaria() { return descricaoAvaria; }
}
