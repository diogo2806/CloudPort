package br.com.cloudport.servicoyard.estivagembulk.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "navio_granel")
public class NavioGranel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "navio_cadastro_id")
    private Long navioCadastroId;

    @Column(name = "versao_perfil", nullable = false)
    private Long versaoPerfil = 1L;

    @Column(name = "versao_navio_canonico")
    private Long versaoNavioCanonico;

    @Column(length = 10)
    private String imo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClasseNavio classe;

    @Column(name = "lpp")
    private Double lpp;

    private Double boca;

    private Double calado;

    private Double deslocamento;

    private Double gm = 1.5;

    @Column(name = "bm_max_permitido")
    private Double bmMaxPermitido;

    @Column(name = "sf_max_permitido")
    private Double sfMaxPermitido;

    @Column(name = "is_template")
    private boolean isTemplate;

    @OneToMany(mappedBy = "navio", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PoraoNavio> poroes = new ArrayList<>();

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

    public Long getNavioCadastroId() {
        return navioCadastroId;
    }

    public void setNavioCadastroId(Long navioCadastroId) {
        this.navioCadastroId = navioCadastroId;
    }

    public Long getVersaoPerfil() {
        return versaoPerfil;
    }

    public void setVersaoPerfil(Long versaoPerfil) {
        this.versaoPerfil = versaoPerfil;
    }

    public Long getVersaoNavioCanonico() {
        return versaoNavioCanonico;
    }

    public void setVersaoNavioCanonico(Long versaoNavioCanonico) {
        this.versaoNavioCanonico = versaoNavioCanonico;
    }

    public String getImo() {
        return imo;
    }

    public void setImo(String imo) {
        this.imo = imo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public ClasseNavio getClasse() {
        return classe;
    }

    public void setClasse(ClasseNavio classe) {
        this.classe = classe;
    }

    public Double getLpp() {
        return lpp;
    }

    public void setLpp(Double lpp) {
        this.lpp = lpp;
    }

    public Double getBoca() {
        return boca;
    }

    public void setBoca(Double boca) {
        this.boca = boca;
    }

    public Double getCalado() {
        return calado;
    }

    public void setCalado(Double calado) {
        this.calado = calado;
    }

    public Double getDeslocamento() {
        return deslocamento;
    }

    public void setDeslocamento(Double deslocamento) {
        this.deslocamento = deslocamento;
    }

    public Double getGm() {
        return gm;
    }

    public void setGm(Double gm) {
        this.gm = gm;
    }

    public Double getBmMaxPermitido() {
        return bmMaxPermitido;
    }

    public void setBmMaxPermitido(Double bmMaxPermitido) {
        this.bmMaxPermitido = bmMaxPermitido;
    }

    public Double getSfMaxPermitido() {
        return sfMaxPermitido;
    }

    public void setSfMaxPermitido(Double sfMaxPermitido) {
        this.sfMaxPermitido = sfMaxPermitido;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
    }

    public List<PoraoNavio> getPoroes() {
        return poroes;
    }

    public void setPoroes(List<PoraoNavio> poroes) {
        this.poroes = poroes;
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
