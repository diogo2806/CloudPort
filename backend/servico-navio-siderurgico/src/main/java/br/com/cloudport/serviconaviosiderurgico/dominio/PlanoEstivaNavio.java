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

@Entity
@Table(name = "plano_estiva_navio")
public class PlanoEstivaNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_navio_id", nullable = false)
    private VisitaNavio visitaNavio;

    @Column(nullable = false)
    private Integer versao = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPlanoEstivaNavio status = StatusPlanoEstivaNavio.RASCUNHO;

    @Column(name = "peso_total_planejado", nullable = false, precision = 14, scale = 3)
    private BigDecimal pesoTotalPlanejado = BigDecimal.ZERO;

    @Column(name = "peso_total_realizado", nullable = false, precision = 14, scale = 3)
    private BigDecimal pesoTotalRealizado = BigDecimal.ZERO;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "validado_em")
    private LocalDateTime validadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public VisitaNavio getVisitaNavio() { return visitaNavio; }
    public void setVisitaNavio(VisitaNavio visitaNavio) { this.visitaNavio = visitaNavio; }
    public Integer getVersao() { return versao; }
    public void setVersao(Integer versao) { this.versao = versao; }
    public StatusPlanoEstivaNavio getStatus() { return status; }
    public void setStatus(StatusPlanoEstivaNavio status) { this.status = status; }
    public BigDecimal getPesoTotalPlanejado() { return pesoTotalPlanejado; }
    public void setPesoTotalPlanejado(BigDecimal pesoTotalPlanejado) { this.pesoTotalPlanejado = pesoTotalPlanejado; }
    public BigDecimal getPesoTotalRealizado() { return pesoTotalRealizado; }
    public void setPesoTotalRealizado(BigDecimal pesoTotalRealizado) { this.pesoTotalRealizado = pesoTotalRealizado; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public LocalDateTime getValidadoEm() { return validadoEm; }
    public void setValidadoEm(LocalDateTime validadoEm) { this.validadoEm = validadoEm; }
}
