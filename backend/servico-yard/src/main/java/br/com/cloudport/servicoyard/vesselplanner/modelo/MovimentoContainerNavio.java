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
@Table(name = "movimento_container_navio")
public class MovimentoContainerNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estivagem_plan_id", nullable = false)
    private EstivagemPlan estivagem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_navio_id", nullable = false)
    private SlotNavio slot;

    @Column(name = "ordem_sequencia", nullable = false)
    private int ordemSequencia;

    @Column(name = "codigo_container", nullable = false, length = 20)
    private String codigoContainer;

    @Column(name = "tipo_operacao", nullable = false, length = 20)
    private String tipoOperacao;

    @Column(name = "guindaste_id", nullable = false)
    private int guindasteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMovimentoContainerNavio status = StatusMovimentoContainerNavio.EM_EXECUCAO;

    @Column(name = "iniciado_por", nullable = false, length = 120)
    private String iniciadoPor;

    @Column(name = "concluido_por", length = 120)
    private String concluidoPor;

    @Column(length = 500)
    private String observacao;

    @Column(name = "iniciado_em", nullable = false)
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

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
        if (iniciadoEm == null) {
            iniciadoEm = atualizadoEm;
        }
        codigoContainer = codigoContainer == null ? null : codigoContainer.trim().toUpperCase();
        tipoOperacao = tipoOperacao == null ? null : tipoOperacao.trim().toUpperCase();
    }
}
