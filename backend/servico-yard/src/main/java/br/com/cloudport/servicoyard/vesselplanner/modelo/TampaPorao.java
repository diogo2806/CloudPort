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
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "tampa_porao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tampa_porao_plano_codigo",
                columnNames = {"estivagem_plan_id", "codigo"}))
public class TampaPorao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estivagem_plan_id", nullable = false)
    private EstivagemPlan estivagem;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "bay_inicial", nullable = false)
    private int bayInicial;

    @Column(name = "bay_final", nullable = false)
    private int bayFinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PosicaoTampaPorao posicao = PosicaoTampaPorao.FECHADA;

    @Column(name = "recurso_atual", length = 80)
    private String recursoAtual;

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
        codigo = codigo == null ? null : codigo.trim().toUpperCase();
        if (posicao == null) {
            posicao = PosicaoTampaPorao.FECHADA;
        }
    }
}
