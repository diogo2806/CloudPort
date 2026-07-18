package br.com.cloudport.servicoyard.inventario.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "tipo_equipamento_inventario")
public class TipoEquipamentoInventario {

    public enum CategoriaEquipamento {
        CONTEINER,
        CHASSI,
        CARRETA,
        ACESSORIO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(name = "descricao", nullable = false, length = 120)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaEquipamento categoria;

    @Column(name = "codigo_iso", length = 10)
    private String codigoIso;

    @Column(name = "comprimento_mm")
    private Integer comprimentoMm;

    @Column(name = "largura_mm")
    private Integer larguraMm;

    @Column(name = "altura_mm")
    private Integer alturaMm;

    @Column(name = "tara_kg", precision = 12, scale = 3)
    private BigDecimal taraKg;

    @Column(name = "capacidade_kg", precision = 12, scale = 3)
    private BigDecimal capacidadeKg;

    @Column(name = "refrigerado", nullable = false)
    private boolean refrigerado;

    @Column(name = "grupo_equivalencia", length = 60)
    private String grupoEquivalencia;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public CategoriaEquipamento getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaEquipamento categoria) {
        this.categoria = categoria;
    }

    public String getCodigoIso() {
        return codigoIso;
    }

    public void setCodigoIso(String codigoIso) {
        this.codigoIso = codigoIso;
    }

    public Integer getComprimentoMm() {
        return comprimentoMm;
    }

    public void setComprimentoMm(Integer comprimentoMm) {
        this.comprimentoMm = comprimentoMm;
    }

    public Integer getLarguraMm() {
        return larguraMm;
    }

    public void setLarguraMm(Integer larguraMm) {
        this.larguraMm = larguraMm;
    }

    public Integer getAlturaMm() {
        return alturaMm;
    }

    public void setAlturaMm(Integer alturaMm) {
        this.alturaMm = alturaMm;
    }

    public BigDecimal getTaraKg() {
        return taraKg;
    }

    public void setTaraKg(BigDecimal taraKg) {
        this.taraKg = taraKg;
    }

    public BigDecimal getCapacidadeKg() {
        return capacidadeKg;
    }

    public void setCapacidadeKg(BigDecimal capacidadeKg) {
        this.capacidadeKg = capacidadeKg;
    }

    public boolean isRefrigerado() {
        return refrigerado;
    }

    public void setRefrigerado(boolean refrigerado) {
        this.refrigerado = refrigerado;
    }

    public String getGrupoEquivalencia() {
        return grupoEquivalencia;
    }

    public void setGrupoEquivalencia(String grupoEquivalencia) {
        this.grupoEquivalencia = grupoEquivalencia;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
