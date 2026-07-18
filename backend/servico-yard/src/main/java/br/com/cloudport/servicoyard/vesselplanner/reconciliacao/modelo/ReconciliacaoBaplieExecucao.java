package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo;

import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reconciliacao_baplie_execucao")
public class ReconciliacaoBaplieExecucao {

    public enum StatusReconciliacao {
        EM_ANALISE,
        COM_DIVERGENCIAS,
        BLOQUEADA,
        CONCLUIDA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estivagem_plan_id", nullable = false)
    private EstivagemPlan plano;

    @Column(name = "bay_plan_id", nullable = false)
    private Long bayPlanId;

    @Column(name = "visita_navio_id")
    private Long visitaNavioId;

    @Column(name = "versao_plano")
    private Long versaoPlano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusReconciliacao status = StatusReconciliacao.EM_ANALISE;

    @Column(name = "total_unidades", nullable = false)
    private int totalUnidades;

    @Column(name = "total_divergencias", nullable = false)
    private int totalDivergencias;

    @Column(name = "total_criticas_abertas", nullable = false)
    private int totalCriticasAbertas;

    @Column(nullable = false, length = 120)
    private String solicitante;

    @Column(name = "executada_em", nullable = false)
    private LocalDateTime executadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "reconciliacao", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<DivergenciaReconciliacao> divergencias = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void touch() {
        if (executadaEm == null) {
            executadaEm = LocalDateTime.now();
        }
        recalcularTotais();
    }

    public void adicionarDivergencia(DivergenciaReconciliacao divergencia) {
        divergencia.setReconciliacao(this);
        divergencias.add(divergencia);
        recalcularTotais();
    }

    public void recalcularTotais() {
        totalDivergencias = divergencias.size();
        totalCriticasAbertas = (int) divergencias.stream()
                .filter(DivergenciaReconciliacao::isCriticaAberta)
                .count();
        long abertas = divergencias.stream()
                .filter(DivergenciaReconciliacao::isAberta)
                .count();
        if (totalCriticasAbertas > 0) {
            status = StatusReconciliacao.BLOQUEADA;
            concluidaEm = null;
        } else if (abertas > 0) {
            status = StatusReconciliacao.COM_DIVERGENCIAS;
            concluidaEm = null;
        } else {
            status = StatusReconciliacao.CONCLUIDA;
            if (concluidaEm == null) {
                concluidaEm = LocalDateTime.now();
            }
        }
    }

    public boolean bloqueiaOperacao() {
        return totalCriticasAbertas > 0;
    }
}
