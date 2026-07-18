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
@Table(name = "item_operacao_stuff_unstuff")
public class ItemOperacaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id", nullable = false)
    private OperacaoStuffUnstuff operacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCarga lote;

    @Column(name = "quantidade_planejada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadePlanejada;

    @Column(name = "quantidade_realizada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeRealizada = BigDecimal.ZERO;

    @Column(name = "volume_planejado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumePlanejadoM3;

    @Column(name = "volume_realizado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeRealizadoM3 = BigDecimal.ZERO;

    @Column(name = "peso_planejado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoPlanejadoKg;

    @Column(name = "peso_realizado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoRealizadoKg = BigDecimal.ZERO;

    @Column(name = "codigo_avaria", length = 80)
    private String codigoAvaria;

    @Column(name = "descricao_avaria", length = 1000)
    private String descricaoAvaria;

    @Column(length = 1000)
    private String divergencia;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }

    public void registrarExecucao(BigDecimal quantidade, BigDecimal volume, BigDecimal peso,
            String codigoAvaria, String descricaoAvaria, String divergencia) {
        BigDecimal novaQuantidade = quantidadeRealizada.add(quantidade);
        BigDecimal novoVolume = volumeRealizadoM3.add(volume);
        BigDecimal novoPeso = pesoRealizadoKg.add(peso);
        if (novaQuantidade.compareTo(quantidadePlanejada) > 0
                || novoVolume.compareTo(volumePlanejadoM3) > 0
                || novoPeso.compareTo(pesoPlanejadoKg) > 0) {
            throw new IllegalStateException("Execução excede o planejamento do item.");
        }
        quantidadeRealizada = novaQuantidade;
        volumeRealizadoM3 = novoVolume;
        pesoRealizadoKg = novoPeso;
        if (codigoAvaria != null && !codigoAvaria.isBlank()) this.codigoAvaria = codigoAvaria.trim().toUpperCase();
        if (descricaoAvaria != null && !descricaoAvaria.isBlank()) this.descricaoAvaria = descricaoAvaria.trim();
        if (divergencia != null && !divergencia.isBlank()) this.divergencia = divergencia.trim();
    }

    public boolean possuiExecucao() {
        return quantidadeRealizada.signum() > 0 || volumeRealizadoM3.signum() > 0 || pesoRealizadoKg.signum() > 0;
    }

    public boolean estaCompleto() {
        return quantidadeRealizada.compareTo(quantidadePlanejada) == 0
                && volumeRealizadoM3.compareTo(volumePlanejadoM3) == 0
                && pesoRealizadoKg.compareTo(pesoPlanejadoKg) == 0;
    }

    public UUID getId() { return id; }
    public OperacaoStuffUnstuff getOperacao() { return operacao; }
    public void setOperacao(OperacaoStuffUnstuff operacao) { this.operacao = operacao; }
    public LoteCarga getLote() { return lote; }
    public void setLote(LoteCarga lote) { this.lote = lote; }
    public BigDecimal getQuantidadePlanejada() { return quantidadePlanejada; }
    public void setQuantidadePlanejada(BigDecimal quantidadePlanejada) { this.quantidadePlanejada = quantidadePlanejada; }
    public BigDecimal getQuantidadeRealizada() { return quantidadeRealizada; }
    public BigDecimal getVolumePlanejadoM3() { return volumePlanejadoM3; }
    public void setVolumePlanejadoM3(BigDecimal volumePlanejadoM3) { this.volumePlanejadoM3 = volumePlanejadoM3; }
    public BigDecimal getVolumeRealizadoM3() { return volumeRealizadoM3; }
    public BigDecimal getPesoPlanejadoKg() { return pesoPlanejadoKg; }
    public void setPesoPlanejadoKg(BigDecimal pesoPlanejadoKg) { this.pesoPlanejadoKg = pesoPlanejadoKg; }
    public BigDecimal getPesoRealizadoKg() { return pesoRealizadoKg; }
    public String getCodigoAvaria() { return codigoAvaria; }
    public String getDescricaoAvaria() { return descricaoAvaria; }
    public String getDivergencia() { return divergencia; }
}
