package br.com.cloudport.servicorail.ferrovia.movimento.modelo;

import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "movimento_ferroviario_interno")
public class MovimentoFerroviarioInterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_movimento", nullable = false, unique = true, length = 36)
    private String codigoMovimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_trem_id", nullable = false)
    private VisitaTrem visitaTrem;

    @Column(nullable = false, length = 120)
    private String origem;

    @Column(nullable = false, length = 120)
    private String destino;

    @Column(name = "inicio_planejado", nullable = false)
    private LocalDateTime inicioPlanejado;

    @Column(name = "fim_planejado", nullable = false)
    private LocalDateTime fimPlanejado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoMovimentoFerroviarioInterno estado = EstadoMovimentoFerroviarioInterno.PLANEJADO;

    @Column(name = "reserva_ativa", nullable = false)
    private boolean reservaAtiva;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "planejado_por", nullable = false, length = 120)
    private String planejadoPor;

    @Column(name = "autorizado_por", length = 120)
    private String autorizadoPor;

    @Column(name = "iniciado_por", length = 120)
    private String iniciadoPor;

    @Column(name = "concluido_por", length = 120)
    private String concluidoPor;

    @Column(name = "cancelado_por", length = 120)
    private String canceladoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "autorizado_em")
    private LocalDateTime autorizadoEm;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "movimento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tipoRecurso ASC, codigoRecurso ASC")
    private List<ReservaRecursoFerroviario> recursos = new ArrayList<>();

    protected MovimentoFerroviarioInterno() {
    }

    public MovimentoFerroviarioInterno(VisitaTrem visitaTrem,
                                        String origem,
                                        String destino,
                                        LocalDateTime inicioPlanejado,
                                        LocalDateTime fimPlanejado,
                                        String planejadoPor) {
        this.visitaTrem = visitaTrem;
        this.origem = origem;
        this.destino = destino;
        this.inicioPlanejado = inicioPlanejado;
        this.fimPlanejado = fimPlanejado;
        this.planejadoPor = planejadoPor;
        this.estado = EstadoMovimentoFerroviarioInterno.PLANEJADO;
        this.reservaAtiva = false;
    }

    public void adicionarRecurso(TipoRecursoFerroviario tipo, String codigo) {
        exigirEstado(EstadoMovimentoFerroviarioInterno.PLANEJADO);
        ReservaRecursoFerroviario recurso = new ReservaRecursoFerroviario(
                tipo,
                codigo,
                inicioPlanejado,
                fimPlanejado);
        recurso.vincular(this);
        recursos.add(recurso);
    }

    public void autorizar(String usuario) {
        exigirEstado(EstadoMovimentoFerroviarioInterno.PLANEJADO);
        if (recursos.isEmpty()) {
            throw new IllegalStateException("O movimento deve possuir ao menos um recurso ferroviário.");
        }
        estado = EstadoMovimentoFerroviarioInterno.AUTORIZADO;
        reservaAtiva = true;
        autorizadoPor = usuario;
        autorizadoEm = LocalDateTime.now();
        recursos.forEach(ReservaRecursoFerroviario::ativar);
    }

    public void iniciar(String usuario) {
        exigirEstado(EstadoMovimentoFerroviarioInterno.AUTORIZADO);
        estado = EstadoMovimentoFerroviarioInterno.EM_EXECUCAO;
        iniciadoPor = usuario;
        iniciadoEm = LocalDateTime.now();
    }

    public void concluir(String usuario) {
        exigirEstado(EstadoMovimentoFerroviarioInterno.EM_EXECUCAO);
        estado = EstadoMovimentoFerroviarioInterno.CONCLUIDO;
        concluidoPor = usuario;
        concluidoEm = LocalDateTime.now();
        liberarRecursos();
    }

    public void cancelar(String motivo, String usuario) {
        if (estado == EstadoMovimentoFerroviarioInterno.CONCLUIDO
                || estado == EstadoMovimentoFerroviarioInterno.CANCELADO) {
            throw new IllegalStateException("Movimento ferroviário em estado terminal não pode ser cancelado.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo do cancelamento deve ser informado.");
        }
        estado = EstadoMovimentoFerroviarioInterno.CANCELADO;
        motivoCancelamento = motivo.trim();
        canceladoPor = usuario;
        canceladoEm = LocalDateTime.now();
        liberarRecursos();
    }

    private void liberarRecursos() {
        reservaAtiva = false;
        recursos.forEach(ReservaRecursoFerroviario::desativar);
    }

    private void exigirEstado(EstadoMovimentoFerroviarioInterno esperado) {
        if (estado != esperado) {
            throw new IllegalStateException(
                    "Transição inválida para movimento ferroviário no estado " + estado + ".");
        }
    }

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (codigoMovimento == null) {
            codigoMovimento = UUID.randomUUID().toString();
        }
        criadoEm = agora;
        atualizadoEm = agora;
        validarJanela();
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
        validarJanela();
    }

    private void validarJanela() {
        if (inicioPlanejado == null || fimPlanejado == null || !fimPlanejado.isAfter(inicioPlanejado)) {
            throw new IllegalStateException("A janela do movimento ferroviário é inválida.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCodigoMovimento() {
        return codigoMovimento;
    }

    public VisitaTrem getVisitaTrem() {
        return visitaTrem;
    }

    public String getOrigem() {
        return origem;
    }

    public String getDestino() {
        return destino;
    }

    public LocalDateTime getInicioPlanejado() {
        return inicioPlanejado;
    }

    public LocalDateTime getFimPlanejado() {
        return fimPlanejado;
    }

    public EstadoMovimentoFerroviarioInterno getEstado() {
        return estado;
    }

    public boolean isReservaAtiva() {
        return reservaAtiva;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public String getPlanejadoPor() {
        return planejadoPor;
    }

    public String getAutorizadoPor() {
        return autorizadoPor;
    }

    public String getIniciadoPor() {
        return iniciadoPor;
    }

    public String getConcluidoPor() {
        return concluidoPor;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public LocalDateTime getAutorizadoEm() {
        return autorizadoEm;
    }

    public LocalDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public Long getVersao() {
        return versao;
    }

    public List<ReservaRecursoFerroviario> getRecursos() {
        return Collections.unmodifiableList(recursos);
    }
}
