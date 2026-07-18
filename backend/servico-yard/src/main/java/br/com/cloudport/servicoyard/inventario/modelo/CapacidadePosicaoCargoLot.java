package br.com.cloudport.servicoyard.inventario.modelo;

import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "capacidade_posicao_cargo_lot")
public class CapacidadePosicaoCargoLot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "posicao", nullable = false, unique = true, length = 120)
    private String posicao;

    @Column(name = "capacidade_quantidade", nullable = false, precision = 19, scale = 3)
    private BigDecimal capacidadeQuantidade;

    @Column(name = "capacidade_volume_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal capacidadeVolumeM3;

    @Column(name = "capacidade_peso_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal capacidadePesoKg;

    @Column(name = "restricoes", length = 1000)
    private String restricoes;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    public UUID getId() { return id; }
    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = posicao == null ? null : posicao.trim().toUpperCase(); }
    public BigDecimal getCapacidadeQuantidade() { return capacidadeQuantidade; }
    public void setCapacidadeQuantidade(BigDecimal capacidadeQuantidade) { this.capacidadeQuantidade = capacidadeQuantidade; }
    public BigDecimal getCapacidadeVolumeM3() { return capacidadeVolumeM3; }
    public void setCapacidadeVolumeM3(BigDecimal capacidadeVolumeM3) { this.capacidadeVolumeM3 = capacidadeVolumeM3; }
    public BigDecimal getCapacidadePesoKg() { return capacidadePesoKg; }
    public void setCapacidadePesoKg(BigDecimal capacidadePesoKg) { this.capacidadePesoKg = capacidadePesoKg; }
    public String getRestricoes() { return restricoes; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public long getVersao() { return versao; }
}
