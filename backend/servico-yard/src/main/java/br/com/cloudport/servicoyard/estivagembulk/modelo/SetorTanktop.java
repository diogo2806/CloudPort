package br.com.cloudport.servicoyard.estivagembulk.modelo;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "setor_tanktop")
public class SetorTanktop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porao_id", nullable = false)
    private PoraoNavio porao;

    @Column(length = 20)
    private String nome;

    @Column(name = "capacidade_t_m2", nullable = false)
    private Double capacidadeTM2;

    @Column(name = "area_m2")
    private Double areaM2;

    @Column(name = "pos_long_inicio")
    private Double posLongInicio;

    @Column(name = "pos_long_fim")
    private Double posLongFim;

    @Column(name = "pos_trans_inicio")
    private Double posTransInicio;

    @Column(name = "pos_trans_fim")
    private Double posTransFim;

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

    public PoraoNavio getPorao() {
        return porao;
    }

    public void setPorao(PoraoNavio porao) {
        this.porao = porao;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getCapacidadeTM2() {
        return capacidadeTM2;
    }

    public void setCapacidadeTM2(Double capacidadeTM2) {
        this.capacidadeTM2 = capacidadeTM2;
    }

    public Double getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(Double areaM2) {
        this.areaM2 = areaM2;
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

    public Double getPosTransInicio() {
        return posTransInicio;
    }

    public void setPosTransInicio(Double posTransInicio) {
        this.posTransInicio = posTransInicio;
    }

    public Double getPosTransFim() {
        return posTransFim;
    }

    public void setPosTransFim(Double posTransFim) {
        this.posTransFim = posTransFim;
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
