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
@Table(name = "item_cargo_siderurgico")
public class ItemCargoSiderurgico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id", nullable = false)
    private PlanoEstivaBulk plano;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", length = 20, nullable = false)
    private TipoCargaSiderurgica tipoCarga;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "heat_number", length = 20)
    private String heatNumber;

    @Column(name = "ordem_venda_erp", length = 20)
    private String ordemVendaErp;

    @Column(name = "numero_corrida", length = 20)
    private String numeroCorrida;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "comprimento_mm")
    private Double comprimentoMm;

    @Column(name = "largura_mm")
    private Double larguraMm;

    @Column(name = "altura_mm")
    private Double alturaMm;

    @Column(name = "diametro_externo_mm")
    private Double diametroExternoMm;

    @Column(name = "grau_aco", length = 20)
    private String grauAco;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Column(name = "requer_berceiro")
    private Boolean requerBerceiro = false;

    @Column(name = "requer_reforco_piso")
    private Boolean requerReforcoPiso = false;

    @Column(name = "max_camadas_empilhamento")
    private Integer maxCamadasEmpilhamento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlanoEstivaBulk getPlano() { return plano; }
    public void setPlano(PlanoEstivaBulk plano) { this.plano = plano; }

    public TipoCargaSiderurgica getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(TipoCargaSiderurgica tipoCarga) { this.tipoCarga = tipoCarga; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getHeatNumber() { return heatNumber; }
    public void setHeatNumber(String heatNumber) { this.heatNumber = heatNumber; }

    public String getOrdemVendaErp() { return ordemVendaErp; }
    public void setOrdemVendaErp(String ordemVendaErp) { this.ordemVendaErp = ordemVendaErp; }

    public String getNumeroCorrida() { return numeroCorrida; }
    public void setNumeroCorrida(String numeroCorrida) { this.numeroCorrida = numeroCorrida; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public Double getComprimentoMm() { return comprimentoMm; }
    public void setComprimentoMm(Double comprimentoMm) { this.comprimentoMm = comprimentoMm; }

    public Double getLarguraMm() { return larguraMm; }
    public void setLarguraMm(Double larguraMm) { this.larguraMm = larguraMm; }

    public Double getAlturaMm() { return alturaMm; }
    public void setAlturaMm(Double alturaMm) { this.alturaMm = alturaMm; }

    public Double getDiametroExternoMm() { return diametroExternoMm; }
    public void setDiametroExternoMm(Double diametroExternoMm) { this.diametroExternoMm = diametroExternoMm; }

    public String getGrauAco() { return grauAco; }
    public void setGrauAco(String grauAco) { this.grauAco = grauAco; }

    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }

    public Boolean getRequerBerceiro() { return requerBerceiro; }
    public void setRequerBerceiro(Boolean requerBerceiro) { this.requerBerceiro = requerBerceiro; }

    public Boolean getRequerReforcoPiso() { return requerReforcoPiso; }
    public void setRequerReforcoPiso(Boolean requerReforcoPiso) { this.requerReforcoPiso = requerReforcoPiso; }

    public Integer getMaxCamadasEmpilhamento() { return maxCamadasEmpilhamento; }
    public void setMaxCamadasEmpilhamento(Integer maxCamadasEmpilhamento) { this.maxCamadasEmpilhamento = maxCamadasEmpilhamento; }
}
