package br.com.cloudport.servicoyard.vesselplanner.modelo;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "divergencia_reconciliacao_slot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_divergencia_reconciliacao_chave",
                columnNames = {"estivagem_plan_id", "chave"}))
public class DivergenciaReconciliacaoSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estivagem_plan_id", nullable = false)
    private EstivagemPlan estivagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_navio_id")
    private SlotNavio slot;

    @Column(name = "chave", nullable = false, length = 160)
    private String chave;

    @Column(name = "codigo_container", nullable = false, length = 40)
    private String codigoContainer;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoDivergenciaReconciliacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private SeveridadeDivergenciaReconciliacao severidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusDivergenciaReconciliacao status = StatusDivergenciaReconciliacao.ABERTA;

    @Column(name = "valor_baplie", columnDefinition = "TEXT")
    private String valorBaplie;

    @Column(name = "valor_plano", columnDefinition = "TEXT")
    private String valorPlano;

    @Column(name = "valor_inventario", columnDefinition = "TEXT")
    private String valorInventario;

    @Column(name = "valor_execucao", columnDefinition = "TEXT")
    private String valorExecucao;

    @Column(name = "assinatura_fontes", nullable = false, length = 64)
    private String assinaturaFontes;

    @Enumerated(EnumType.STRING)
    @Column(name = "decisao", length = 50)
    private DecisaoResolucaoReconciliacao decisao;

    @Column(name = "justificativa", length = 1000)
    private String justificativa;

    @Column(name = "resolvido_por", length = 150)
    private String resolvidoPor;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
        if (status == null) {
            status = StatusDivergenciaReconciliacao.ABERTA;
        }
    }
}
