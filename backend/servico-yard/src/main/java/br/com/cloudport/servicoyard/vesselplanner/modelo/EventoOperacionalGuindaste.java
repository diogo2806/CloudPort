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
@Table(name = "evento_operacional_guindaste")
public class EventoOperacionalGuindaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execucao_id", nullable = false)
    private ExecucaoSequenciaGuindaste execucao;

    @Column(name = "guindaste_id", nullable = false)
    private Integer guindasteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoOperacionalGuindaste tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NaturezaParalisacaoGuindaste natureza;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime fim;

    @Column(length = 1000)
    private String motivo;

    @Column(length = 1000)
    private String impacto;

    @Column(name = "turno_origem", length = 120)
    private String turnoOrigem;

    @Column(name = "turno_destino", length = 120)
    private String turnoDestino;

    @Column(nullable = false, length = 120)
    private String responsavel;

    @Column(name = "responsavel_destino", length = 120)
    private String responsavelDestino;

    @Column(length = 2000)
    private String pendencias;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "encerrado_por", length = 120)
    private String encerradoPor;

    @Column(name = "observacao_encerramento", length = 1000)
    private String observacaoEncerramento;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public boolean aberta() {
        return tipo == TipoEventoOperacionalGuindaste.PARALISACAO && fim == null;
    }

    public String estado() {
        if (tipo == TipoEventoOperacionalGuindaste.HANDOVER) {
            return "REGISTRADO";
        }
        return aberta() ? "ABERTA" : "ENCERRADA";
    }

    public void encerrar(LocalDateTime encerramento, String usuario, String observacaoFinal) {
        if (tipo != TipoEventoOperacionalGuindaste.PARALISACAO) {
            throw new IllegalStateException("Somente paralisações podem ser encerradas.");
        }
        if (!aberta()) {
            throw new IllegalStateException("A paralisação já está encerrada.");
        }
        LocalDateTime instante = encerramento == null ? LocalDateTime.now() : encerramento;
        if (!instante.isAfter(inicio)) {
            throw new IllegalStateException("O fim da paralisação deve ser posterior ao início.");
        }
        fim = instante;
        encerradoPor = usuario;
        observacaoEncerramento = observacaoFinal;
    }

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
}
