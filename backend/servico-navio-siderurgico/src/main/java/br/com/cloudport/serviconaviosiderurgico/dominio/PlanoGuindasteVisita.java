package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "plano_guindaste_visita",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plano_guindaste_visita_sequencia",
                columnNames = {"visita_navio_id", "sequencia"}))
public class PlanoGuindasteVisita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_navio_id", nullable = false)
    private VisitaNavio visitaNavio;

    @Column(name = "codigo_guindaste", nullable = false, length = 40)
    private String codigoGuindaste;

    @Column(name = "recurso_cais", length = 80)
    private String recursoCais;

    @Column(nullable = false)
    private Integer porao;

    @Column(name = "work_queue_id")
    private Long workQueueId;

    @Column(nullable = false)
    private Integer sequencia;

    @Column(name = "movimentos_planejados", nullable = false)
    private Integer movimentosPlanejados;

    @Column(name = "produtividade_planejada_mph", nullable = false, precision = 10, scale = 2)
    private BigDecimal produtividadePlanejadaMovimentosHora;

    @Column(name = "inicio_planejado", nullable = false)
    private LocalDateTime inicioPlanejado;

    @Column(name = "fim_planejado", nullable = false)
    private LocalDateTime fimPlanejado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPlanoGuindaste status = StatusPlanoGuindaste.RASCUNHO;

    @Column(nullable = false, length = 40)
    private String berco;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        if (status == null) {
            status = StatusPlanoGuindaste.RASCUNHO;
        }
        if (usuario == null || usuario.isBlank()) {
            usuario = "sistema";
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        if (status == null) {
            status = StatusPlanoGuindaste.RASCUNHO;
        }
        if (usuario == null || usuario.isBlank()) {
            usuario = "sistema";
        }
    }

    public Long getId() { return id; }
    public VisitaNavio getVisitaNavio() { return visitaNavio; }
    public void setVisitaNavio(VisitaNavio visitaNavio) { this.visitaNavio = visitaNavio; }
    public String getCodigoGuindaste() { return codigoGuindaste; }
    public void setCodigoGuindaste(String codigoGuindaste) { this.codigoGuindaste = codigoGuindaste; }
    public String getRecursoCais() { return recursoCais; }
    public void setRecursoCais(String recursoCais) { this.recursoCais = recursoCais; }
    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }
    public Integer getMovimentosPlanejados() { return movimentosPlanejados; }
    public void setMovimentosPlanejados(Integer movimentosPlanejados) { this.movimentosPlanejados = movimentosPlanejados; }
    public BigDecimal getProdutividadePlanejadaMovimentosHora() { return produtividadePlanejadaMovimentosHora; }
    public void setProdutividadePlanejadaMovimentosHora(BigDecimal produtividadePlanejadaMovimentosHora) { this.produtividadePlanejadaMovimentosHora = produtividadePlanejadaMovimentosHora; }
    public LocalDateTime getInicioPlanejado() { return inicioPlanejado; }
    public void setInicioPlanejado(LocalDateTime inicioPlanejado) { this.inicioPlanejado = inicioPlanejado; }
    public LocalDateTime getFimPlanejado() { return fimPlanejado; }
    public void setFimPlanejado(LocalDateTime fimPlanejado) { this.fimPlanejado = fimPlanejado; }
    public StatusPlanoGuindaste getStatus() { return status; }
    public void setStatus(StatusPlanoGuindaste status) { this.status = status; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
