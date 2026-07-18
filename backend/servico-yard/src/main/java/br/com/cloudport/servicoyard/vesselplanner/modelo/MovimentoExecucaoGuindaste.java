package br.com.cloudport.servicoyard.vesselplanner.modelo;

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
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "movimento_execucao_guindaste")
public class MovimentoExecucaoGuindaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execucao_id", nullable = false)
    private ExecucaoSequenciaGuindaste execucao;

    @Column(name = "ordem_planejada", nullable = false)
    private Integer ordemPlanejada;

    @Column(name = "guindaste_id", nullable = false)
    private Integer guindasteId;

    @Column(name = "codigo_container", nullable = false, length = 20)
    private String codigoContainer;

    @Column(nullable = false)
    private Integer bay;

    @Column(name = "row_bay", nullable = false)
    private Integer rowBay;

    @Column(nullable = false)
    private Integer tier;

    @Column(name = "tipo_operacao", nullable = false, length = 30)
    private String tipoOperacao;

    @Column(name = "codigo_hatch_cover", length = 40)
    private String codigoHatchCover;

    @Column(name = "sobre_hatch_cover", nullable = false)
    private boolean sobreHatchCover;

    @Column(name = "janela_inicio_planejada", nullable = false)
    private LocalDateTime janelaInicioPlanejada;

    @Column(name = "janela_fim_planejada", nullable = false)
    private LocalDateTime janelaFimPlanejada;

    @Column(name = "quantidade_planejada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadePlanejada = BigDecimal.ONE;

    @Column(name = "quantidade_realizada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeRealizada = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusMovimentoExecucaoGuindaste status = StatusMovimentoExecucaoGuindaste.PLANEJADO;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "iniciado_por", length = 120)
    private String iniciadoPor;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "concluido_por", length = 120)
    private String concluidoPor;

    @Column(length = 1000)
    private String excecao;

    @Column(name = "motivo_replanejamento", length = 1000)
    private String motivoReplanejamento;

    @Column(name = "replanejado_em")
    private LocalDateTime replanejadoEm;

    @Column(name = "replanejado_por", length = 120)
    private String replanejadoPor;

    @Version
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public void iniciar(LocalDateTime ocorridoEm, String usuario) {
        exigirStatusPlanejavel("iniciado");
        LocalDateTime instante = ocorridoEm != null ? ocorridoEm : LocalDateTime.now();
        status = StatusMovimentoExecucaoGuindaste.EM_EXECUCAO;
        iniciadoEm = instante;
        iniciadoPor = normalizarUsuario(usuario);
        excecao = null;
    }

    public void concluir(BigDecimal quantidade, LocalDateTime ocorridoEm, String usuario) {
        if (status != StatusMovimentoExecucaoGuindaste.EM_EXECUCAO) {
            throw new IllegalStateException("Somente movimento em execução pode ser concluído.");
        }
        validarQuantidadeRealizada(quantidade);
        status = StatusMovimentoExecucaoGuindaste.CONCLUIDO;
        quantidadeRealizada = quantidade;
        concluidoEm = ocorridoEm != null ? ocorridoEm : LocalDateTime.now();
        concluidoPor = normalizarUsuario(usuario);
        excecao = null;
    }

    public void falhar(String descricaoExcecao,
                        BigDecimal quantidade,
                        LocalDateTime ocorridoEm,
                        String usuario) {
        if (status == StatusMovimentoExecucaoGuindaste.CONCLUIDO
                || status == StatusMovimentoExecucaoGuindaste.FALHA) {
            throw new IllegalStateException("Movimento finalizado não pode receber nova falha.");
        }
        if (descricaoExcecao == null || descricaoExcecao.trim().isEmpty()) {
            throw new IllegalArgumentException("A exceção operacional deve ser informada.");
        }
        BigDecimal realizada = quantidade != null ? quantidade : BigDecimal.ZERO;
        validarQuantidadeRealizada(realizada);
        LocalDateTime instante = ocorridoEm != null ? ocorridoEm : LocalDateTime.now();
        if (iniciadoEm == null) {
            iniciadoEm = instante;
            iniciadoPor = normalizarUsuario(usuario);
        }
        status = StatusMovimentoExecucaoGuindaste.FALHA;
        quantidadeRealizada = realizada;
        concluidoEm = instante;
        concluidoPor = normalizarUsuario(usuario);
        excecao = descricaoExcecao.trim();
    }

    public void replanejar(Integer novoGuindasteId,
                            Integer novaOrdem,
                            LocalDateTime novaJanelaInicio,
                            LocalDateTime novaJanelaFim,
                            String motivo,
                            String usuario) {
        exigirStatusPlanejavel("replanejado");
        if (novoGuindasteId == null || novoGuindasteId < 1) {
            throw new IllegalArgumentException("O guindaste do replanejamento deve ser informado.");
        }
        if (novaOrdem == null || novaOrdem < 1) {
            throw new IllegalArgumentException("A ordem do replanejamento deve ser informada.");
        }
        if (novaJanelaInicio == null || novaJanelaFim == null || !novaJanelaInicio.isBefore(novaJanelaFim)) {
            throw new IllegalArgumentException("A janela replanejada deve possuir início anterior ao fim.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo do replanejamento deve ser informado.");
        }
        guindasteId = novoGuindasteId;
        ordemPlanejada = novaOrdem;
        janelaInicioPlanejada = novaJanelaInicio;
        janelaFimPlanejada = novaJanelaFim;
        motivoReplanejamento = motivo.trim();
        replanejadoEm = LocalDateTime.now();
        replanejadoPor = normalizarUsuario(usuario);
        status = StatusMovimentoExecucaoGuindaste.REPLANEJADO;
    }

    public boolean terminal() {
        return status == StatusMovimentoExecucaoGuindaste.CONCLUIDO
                || status == StatusMovimentoExecucaoGuindaste.FALHA;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        validarDadosPlanejados();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        validarDadosPlanejados();
    }

    private void exigirStatusPlanejavel(String acao) {
        if (status != StatusMovimentoExecucaoGuindaste.PLANEJADO
                && status != StatusMovimentoExecucaoGuindaste.REPLANEJADO) {
            throw new IllegalStateException("Movimento no estado " + status + " não pode ser " + acao + ".");
        }
    }

    private void validarDadosPlanejados() {
        if (ordemPlanejada == null || ordemPlanejada < 1) {
            throw new IllegalStateException("Ordem planejada inválida.");
        }
        if (guindasteId == null || guindasteId < 1) {
            throw new IllegalStateException("Guindaste planejado inválido.");
        }
        if (janelaInicioPlanejada == null
                || janelaFimPlanejada == null
                || !janelaInicioPlanejada.isBefore(janelaFimPlanejada)) {
            throw new IllegalStateException("Janela planejada inválida.");
        }
        if (quantidadePlanejada == null || quantidadePlanejada.signum() <= 0) {
            throw new IllegalStateException("Quantidade planejada deve ser positiva.");
        }
        validarQuantidadeRealizada(quantidadeRealizada);
    }

    private void validarQuantidadeRealizada(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() < 0) {
            throw new IllegalArgumentException("Quantidade realizada não pode ser negativa.");
        }
    }

    private String normalizarUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return "SISTEMA";
        }
        return usuario.trim();
    }
}
