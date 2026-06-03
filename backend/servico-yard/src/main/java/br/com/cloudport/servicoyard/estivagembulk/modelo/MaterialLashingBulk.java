package br.com.cloudport.servicoyard.estivagembulk.modelo;

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
import javax.persistence.Table;

@Entity
@Table(name = "material_lashing_bulk")
public class MaterialLashingBulk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id")
    private PlanoEstivaBulk plano;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private TipoLashing tipo;

    private int quantidade;

    @Column(name = "comprimento_m")
    private Double comprimentoM;

    @Column(name = "peso_unitario_kg")
    private Double pesoUnitarioKg;

    @Column(length = 200)
    private String descricao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanoEstivaBulk getPlano() {
        return plano;
    }

    public void setPlano(PlanoEstivaBulk plano) {
        this.plano = plano;
    }

    public TipoLashing getTipo() {
        return tipo;
    }

    public void setTipo(TipoLashing tipo) {
        this.tipo = tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public Double getComprimentoM() {
        return comprimentoM;
    }

    public void setComprimentoM(Double comprimentoM) {
        this.comprimentoM = comprimentoM;
    }

    public Double getPesoUnitarioKg() {
        return pesoUnitarioKg;
    }

    public void setPesoUnitarioKg(Double pesoUnitarioKg) {
        this.pesoUnitarioKg = pesoUnitarioKg;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
