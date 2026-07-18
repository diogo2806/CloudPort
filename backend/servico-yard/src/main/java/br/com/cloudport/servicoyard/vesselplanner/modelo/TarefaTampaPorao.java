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
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tarefa_tampa_porao")
public class TarefaTampaPorao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tampa_porao_id", nullable = false)
    private TampaPorao tampa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTarefaTampaPorao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTarefaTampaPorao status = StatusTarefaTampaPorao.PLANEJADA;

    @Column(name = "ordem_operacional", nullable = false)
    private int ordemOperacional;

    @Column(name = "ordem_movimento_referencia")
    private Integer ordemMovimentoReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_sequencia", nullable = false, length = 10)
    private MomentoSequenciaTampaPorao momentoSequencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependencia_id")
    private TarefaTampaPorao dependencia;

    @Column(length = 80)
    private String recurso;

    @Column(name = "iniciado_por", length = 120)
    private String iniciadoPor;

    @Column(name = "confirmado_por", length = 120)
    private String confirmadoPor;

    @Column(name = "cancelado_por", length = 120)
    private String canceladoPor;

    @Column(length = 500)
    private String observacao;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Version
    @Column(name = "versao_registro")
    private Long versao;

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
            status = StatusTarefaTampaPorao.PLANEJADA;
        }
    }
}
