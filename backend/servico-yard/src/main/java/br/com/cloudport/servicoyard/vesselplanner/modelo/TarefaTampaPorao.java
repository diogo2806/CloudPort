package br.com.cloudport.servicoyard.vesselplanner.modelo;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.StatusTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoPosicaoTampaPorao;
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
    private TipoOperacaoTampaPorao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTarefaTampaPorao status = StatusTarefaTampaPorao.PLANEJADA;

    @Column(nullable = false, length = 120)
    private String recurso;

    @Column(nullable = false, length = 120)
    private String operador;

    @Column(length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "posicao_destino_tipo", length = 30)
    private TipoPosicaoTampaPorao posicaoDestinoTipo;

    @Column(name = "posicao_destino_referencia", length = 120)
    private String posicaoDestinoReferencia;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Version
    private Long versao;

    @PrePersist
    @PreUpdate
    void touch() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        recurso = recurso == null ? null : recurso.trim();
        operador = operador == null ? null : operador.trim();
        motivo = motivo == null ? null : motivo.trim();
        posicaoDestinoReferencia = posicaoDestinoReferencia == null
                ? null : posicaoDestinoReferencia.trim();
    }

    public void iniciar() {
        if (status != StatusTarefaTampaPorao.PLANEJADA) {
            throw new IllegalStateException("Somente tarefas planejadas podem ser iniciadas.");
        }
        status = StatusTarefaTampaPorao.EM_EXECUCAO;
        iniciadoEm = LocalDateTime.now();
    }

    public void concluir() {
        if (status != StatusTarefaTampaPorao.EM_EXECUCAO) {
            throw new IllegalStateException("Somente tarefas em execução podem ser confirmadas.");
        }
        status = StatusTarefaTampaPorao.CONCLUIDA;
        concluidoEm = LocalDateTime.now();
    }

    public void cancelar(String motivoCancelamento) {
        if (status == StatusTarefaTampaPorao.CONCLUIDA || status == StatusTarefaTampaPorao.CANCELADA) {
            throw new IllegalStateException("A tarefa já está encerrada.");
        }
        status = StatusTarefaTampaPorao.CANCELADA;
        canceladoEm = LocalDateTime.now();
        if (motivoCancelamento != null && !motivoCancelamento.trim().isEmpty()) {
            motivo = motivoCancelamento.trim();
        }
    }
}
