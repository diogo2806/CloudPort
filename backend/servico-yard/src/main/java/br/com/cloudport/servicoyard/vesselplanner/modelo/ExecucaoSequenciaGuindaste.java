package br.com.cloudport.servicoyard.vesselplanner.modelo;

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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "execucao_sequencia_guindaste")
public class ExecucaoSequenciaGuindaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estivagem_plan_id", nullable = false, unique = true)
    private EstivagemPlan estivagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusExecucaoSequenciaGuindaste status = StatusExecucaoSequenciaGuindaste.PLANEJADA;

    @Column(name = "numero_guindastes", nullable = false)
    private Integer numeroGuindastes;

    @Column(name = "janela_base_inicio", nullable = false)
    private LocalDateTime janelaBaseInicio;

    @Column(name = "duracao_movimento_minutos", nullable = false)
    private Integer duracaoMovimentoMinutos;

    @Column(name = "reconciliado_em")
    private LocalDateTime reconciliadoEm;

    @Column(name = "reconciliado_por", length = 120)
    private String reconciliadoPor;

    @Column(name = "observacao_reconciliacao", length = 1000)
    private String observacaoReconciliacao;

    @Version
    private Long versao;

    @OneToMany(mappedBy = "execucao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordemPlanejada ASC")
    private List<MovimentoExecucaoGuindaste> movimentos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public void adicionarMovimento(MovimentoExecucaoGuindaste movimento) {
        if (movimento == null) {
            throw new IllegalArgumentException("Movimento de guindaste deve ser informado.");
        }
        movimento.setExecucao(this);
        movimentos.add(movimento);
    }

    public MovimentoExecucaoGuindaste obterMovimento(Long movimentoId) {
        return movimentos.stream()
                .filter(movimento -> movimento.getId().equals(movimentoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Movimento não pertence à execução informada."));
    }

    public void atualizarStatus() {
        if (reconciliadoEm != null) {
            status = StatusExecucaoSequenciaGuindaste.RECONCILIADA;
            return;
        }
        if (!movimentos.isEmpty() && movimentos.stream().allMatch(MovimentoExecucaoGuindaste::terminal)) {
            status = StatusExecucaoSequenciaGuindaste.AGUARDANDO_RECONCILIACAO;
            return;
        }
        if (movimentos.stream().anyMatch(movimento ->
                movimento.getStatus() == StatusMovimentoExecucaoGuindaste.EM_EXECUCAO
                        || movimento.terminal())) {
            status = StatusExecucaoSequenciaGuindaste.EM_EXECUCAO;
            return;
        }
        status = StatusExecucaoSequenciaGuindaste.PLANEJADA;
    }

    public void reconciliar(String observacao, String usuario) {
        if (movimentos.isEmpty()) {
            throw new IllegalStateException("Não existem movimentos para reconciliar.");
        }
        if (!movimentos.stream().allMatch(MovimentoExecucaoGuindaste::terminal)) {
            throw new IllegalStateException("Todos os movimentos devem estar concluídos ou com falha antes da reconciliação.");
        }
        reconciliadoEm = LocalDateTime.now();
        reconciliadoPor = normalizarUsuario(usuario);
        observacaoReconciliacao = normalizarObservacao(observacao);
        status = StatusExecucaoSequenciaGuindaste.RECONCILIADA;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        validarPlanejamento();
        atualizarStatus();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        validarPlanejamento();
        atualizarStatus();
    }

    private void validarPlanejamento() {
        if (numeroGuindastes == null || numeroGuindastes < 1) {
            throw new IllegalStateException("Número de guindastes deve ser positivo.");
        }
        if (janelaBaseInicio == null) {
            throw new IllegalStateException("Janela base de execução deve ser informada.");
        }
        if (duracaoMovimentoMinutos == null || duracaoMovimentoMinutos < 1) {
            throw new IllegalStateException("Duração planejada do movimento deve ser positiva.");
        }
    }

    private String normalizarUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return "SISTEMA";
        }
        return usuario.trim();
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.trim().isEmpty()) {
            return null;
        }
        return observacao.trim();
    }
}
