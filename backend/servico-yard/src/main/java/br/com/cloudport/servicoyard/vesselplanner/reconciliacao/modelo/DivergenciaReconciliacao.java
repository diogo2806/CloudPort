package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo;

import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
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
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "divergencia_reconciliacao_baplie")
public class DivergenciaReconciliacao {

    public enum TipoDivergencia {
        UNIDADE,
        SLOT,
        PESO,
        PORTO,
        PERIGOSO,
        REEFER,
        EXECUCAO,
        POSICAO_FISICA
    }

    public enum SeveridadeDivergencia {
        INFORMATIVA,
        ALERTA,
        CRITICA
    }

    public enum StatusDivergencia {
        ABERTA,
        RESOLVIDA
    }

    public enum FonteDado {
        BAPLIE,
        PLANO_APROVADO,
        INVENTARIO,
        EXECUCAO,
        POSICAO_FISICA
    }

    public enum DecisaoResolucao {
        ACEITAR_BAPLIE,
        ACEITAR_PLANO,
        ACEITAR_INVENTARIO,
        ACEITAR_EXECUCAO,
        CORRECAO_EXTERNA,
        ACEITAR_DIVERGENCIA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliacao_id", nullable = false)
    private ReconciliacaoBaplieExecucao reconciliacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_navio_id")
    private SlotNavio slotNavio;

    @Column(name = "codigo_container", nullable = false, length = 30)
    private String codigoContainer;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_divergencia", nullable = false, length = 30)
    private TipoDivergencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeveridadeDivergencia severidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDivergencia status = StatusDivergencia.ABERTA;

    @Column(nullable = false, length = 60)
    private String campo;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_referencia", nullable = false, length = 30)
    private FonteDado fonteReferencia;

    @Column(name = "valor_referencia", columnDefinition = "TEXT")
    private String valorReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_divergente", nullable = false, length = 30)
    private FonteDado fonteDivergente;

    @Column(name = "valor_divergente", columnDefinition = "TEXT")
    private String valorDivergente;

    @Enumerated(EnumType.STRING)
    @Column(name = "decisao_resolucao", length = 30)
    private DecisaoResolucao decisaoResolucao;

    @Column(name = "motivo_resolucao", length = 1000)
    private String motivoResolucao;

    @Column(name = "responsavel_resolucao", length = 120)
    private String responsavelResolucao;

    @Column(name = "detectada_em", nullable = false)
    private LocalDateTime detectadaEm;

    @Column(name = "resolvida_em")
    private LocalDateTime resolvidaEm;

    @PrePersist
    void prePersist() {
        if (detectadaEm == null) {
            detectadaEm = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusDivergencia.ABERTA;
        }
    }

    public boolean isAberta() {
        return status == StatusDivergencia.ABERTA;
    }

    public boolean isCriticaAberta() {
        return isAberta() && severidade == SeveridadeDivergencia.CRITICA;
    }

    public void resolver(DecisaoResolucao decisao, String motivo, String responsavel) {
        if (!isAberta()) {
            throw new IllegalStateException("A divergência já foi resolvida");
        }
        if (decisao == null) {
            throw new IllegalArgumentException("A decisão de resolução é obrigatória");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("O motivo da resolução é obrigatório");
        }
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalArgumentException("O responsável pela resolução é obrigatório");
        }
        decisaoResolucao = decisao;
        motivoResolucao = motivo.trim();
        responsavelResolucao = responsavel.trim();
        status = StatusDivergencia.RESOLVIDA;
        resolvidaEm = LocalDateTime.now();
    }
}
