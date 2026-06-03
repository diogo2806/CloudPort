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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "posicao_bobina")
public class PosicaoBobina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id")
    private PlanoEstivaBulk plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bobina_id")
    private BobinaManifesto bobina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porao_id")
    private PoraoNavio porao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private SetorTanktop setor;

    @Column(nullable = false)
    private int camada;

    @Column(name = "posicao_x")
    private Double posicaoX;

    @Column(name = "posicao_y")
    private Double posicaoY;

    @Column(name = "angulo_inclinacao")
    private Double anguloInclinacao = 0.0;

    @Column(name = "espessura_dunnage_mm")
    private Double espessuraDunnageMm = 50.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lashing", length = 25)
    private TipoLashing tipoLashing = TipoLashing.SEM_LASHING;

    @Column(name = "alerta_tanktop", length = 200)
    private String alertaTanktop;

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

    public PlanoEstivaBulk getPlano() {
        return plano;
    }

    public void setPlano(PlanoEstivaBulk plano) {
        this.plano = plano;
    }

    public BobinaManifesto getBobina() {
        return bobina;
    }

    public void setBobina(BobinaManifesto bobina) {
        this.bobina = bobina;
    }

    public PoraoNavio getPorao() {
        return porao;
    }

    public void setPorao(PoraoNavio porao) {
        this.porao = porao;
    }

    public SetorTanktop getSetor() {
        return setor;
    }

    public void setSetor(SetorTanktop setor) {
        this.setor = setor;
    }

    public int getCamada() {
        return camada;
    }

    public void setCamada(int camada) {
        this.camada = camada;
    }

    public Double getPosicaoX() {
        return posicaoX;
    }

    public void setPosicaoX(Double posicaoX) {
        this.posicaoX = posicaoX;
    }

    public Double getPosicaoY() {
        return posicaoY;
    }

    public void setPosicaoY(Double posicaoY) {
        this.posicaoY = posicaoY;
    }

    public Double getAnguloInclinacao() {
        return anguloInclinacao;
    }

    public void setAnguloInclinacao(Double anguloInclinacao) {
        this.anguloInclinacao = anguloInclinacao;
    }

    public Double getEspessuraDunnageMm() {
        return espessuraDunnageMm;
    }

    public void setEspessuraDunnageMm(Double espessuraDunnageMm) {
        this.espessuraDunnageMm = espessuraDunnageMm;
    }

    public TipoLashing getTipoLashing() {
        return tipoLashing;
    }

    public void setTipoLashing(TipoLashing tipoLashing) {
        this.tipoLashing = tipoLashing;
    }

    public String getAlertaTanktop() {
        return alertaTanktop;
    }

    public void setAlertaTanktop(String alertaTanktop) {
        this.alertaTanktop = alertaTanktop;
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
