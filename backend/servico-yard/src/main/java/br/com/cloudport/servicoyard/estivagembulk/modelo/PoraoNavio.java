package br.com.cloudport.servicoyard.estivagembulk.modelo;

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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "porao_navio")
public class PoraoNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navio_granel_id", nullable = false)
    private NavioGranel navio;

    @Column(nullable = false)
    private int numero;

    private Double comprimento;

    private Double largura;

    @Column(name = "altura_util")
    private Double alturaUtil;

    @Column(name = "area_util_m2")
    private Double areaUtil;

    @Column(name = "angulo_antepara")
    private Double anguloAntepara;

    @Column(name = "pos_long_inicio_m")
    private Double posLongInicio;

    @Column(name = "pos_long_fim_m")
    private Double posLongFim;

    @OneToMany(mappedBy = "porao", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SetorTanktop> setores = new ArrayList<>();

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NavioGranel getNavio() {
        return navio;
    }

    public void setNavio(NavioGranel navio) {
        this.navio = navio;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Double getComprimento() {
        return comprimento;
    }

    public void setComprimento(Double comprimento) {
        this.comprimento = comprimento;
    }

    public Double getLargura() {
        return largura;
    }

    public void setLargura(Double largura) {
        this.largura = largura;
    }

    public Double getAlturaUtil() {
        return alturaUtil;
    }

    public void setAlturaUtil(Double alturaUtil) {
        this.alturaUtil = alturaUtil;
    }

    public Double getAreaUtil() {
        return areaUtil;
    }

    public void setAreaUtil(Double areaUtil) {
        this.areaUtil = areaUtil;
    }

    public Double getAnguloAntepara() {
        return anguloAntepara;
    }

    public void setAnguloAntepara(Double anguloAntepara) {
        this.anguloAntepara = anguloAntepara;
    }

    public Double getPosLongInicio() {
        return posLongInicio;
    }

    public void setPosLongInicio(Double posLongInicio) {
        this.posLongInicio = posLongInicio;
    }

    public Double getPosLongFim() {
        return posLongFim;
    }

    public void setPosLongFim(Double posLongFim) {
        this.posLongFim = posLongFim;
    }

    public List<SetorTanktop> getSetores() {
        return setores;
    }

    public void setSetores(List<SetorTanktop> setores) {
        this.setores = setores;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
