package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import java.time.OffsetDateTime;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "operacao_stuff_unstuff")
public class OperacaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOperacaoStuffUnstuff tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOperacaoStuffUnstuff status = StatusOperacaoStuffUnstuff.PLANEJADA;

    @Column(name = "conteiner_id", nullable = false, length = 80)
    private String conteinerId;

    @Column(name = "armazem_id", length = 80)
    private String armazemId;

    @Column(name = "posicao_operacao", length = 120)
    private String posicaoOperacao;

    @Column(name = "equipe_recurso", length = 120)
    private String equipeRecurso;

    @Column(name = "lacre_inicial", length = 80)
    private String lacreInicial;

    @Column(name = "lacre_final", length = 80)
    private String lacreFinal;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("criadoEm ASC")
    private List<ItemOperacaoStuffUnstuff> itens = new ArrayList<>();

    @OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm ASC")
    private List<EventoOperacaoStuffUnstuff> historico = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "iniciado_em")
    private OffsetDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        conteinerId = normalizar(conteinerId);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        conteinerId = normalizar(conteinerId);
    }

    public void adicionarItem(ItemOperacaoStuffUnstuff item) {
        item.setOperacao(this);
        itens.add(item);
    }

    public void registrarEvento(TipoEventoStuffUnstuff tipoEvento, String usuario, String correlationId, String descricao) {
        EventoOperacaoStuffUnstuff evento = new EventoOperacaoStuffUnstuff();
        evento.setOperacao(this);
        evento.setTipo(tipoEvento);
        evento.setUsuario(usuario);
        evento.setCorrelationId(correlationId);
        evento.setDescricao(descricao);
        historico.add(evento);
    }

    public void iniciar() {
        if (status != StatusOperacaoStuffUnstuff.PLANEJADA) {
            throw new IllegalStateException("Somente operação planejada pode ser iniciada.");
        }
        status = StatusOperacaoStuffUnstuff.EM_EXECUCAO;
        iniciadoEm = OffsetDateTime.now();
    }

    public void atualizarStatusExecucao() {
        boolean algumaExecucao = itens.stream().anyMatch(ItemOperacaoStuffUnstuff::possuiExecucao);
        boolean completa = itens.stream().allMatch(ItemOperacaoStuffUnstuff::estaCompleto);
        status = completa ? StatusOperacaoStuffUnstuff.EM_EXECUCAO
                : algumaExecucao ? StatusOperacaoStuffUnstuff.PARCIAL : StatusOperacaoStuffUnstuff.EM_EXECUCAO;
    }

    public void concluir(String lacre, String observacao, String usuario, String correlationId) {
        if (status == StatusOperacaoStuffUnstuff.CANCELADA || status == StatusOperacaoStuffUnstuff.CONCLUIDA) {
            throw new IllegalStateException("Operação já está encerrada.");
        }
        if (itens.stream().anyMatch(item -> !item.estaCompleto())) {
            throw new IllegalStateException("Todos os itens devem atingir a quantidade planejada antes da conclusão.");
        }
        lacreFinal = lacre;
        status = StatusOperacaoStuffUnstuff.CONCLUIDA;
        concluidoEm = OffsetDateTime.now();
        registrarEvento(TipoEventoStuffUnstuff.CONCLUIDA, usuario, correlationId, observacao);
    }

    public void cancelar(String motivo, String usuario, String correlationId) {
        if (status == StatusOperacaoStuffUnstuff.CONCLUIDA || status == StatusOperacaoStuffUnstuff.CANCELADA) {
            throw new IllegalStateException("Operação já está encerrada.");
        }
        motivoCancelamento = motivo;
        status = StatusOperacaoStuffUnstuff.CANCELADA;
        canceladoEm = OffsetDateTime.now();
        registrarEvento(TipoEventoStuffUnstuff.CANCELADA, usuario, correlationId, motivo);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public TipoOperacaoStuffUnstuff getTipo() { return tipo; }
    public void setTipo(TipoOperacaoStuffUnstuff tipo) { this.tipo = tipo; }
    public StatusOperacaoStuffUnstuff getStatus() { return status; }
    public String getConteinerId() { return conteinerId; }
    public void setConteinerId(String conteinerId) { this.conteinerId = conteinerId; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getPosicaoOperacao() { return posicaoOperacao; }
    public void setPosicaoOperacao(String posicaoOperacao) { this.posicaoOperacao = posicaoOperacao; }
    public String getEquipeRecurso() { return equipeRecurso; }
    public void setEquipeRecurso(String equipeRecurso) { this.equipeRecurso = equipeRecurso; }
    public String getLacreInicial() { return lacreInicial; }
    public void setLacreInicial(String lacreInicial) { this.lacreInicial = lacreInicial; }
    public String getLacreFinal() { return lacreFinal; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public List<ItemOperacaoStuffUnstuff> getItens() { return Collections.unmodifiableList(itens); }
    public List<EventoOperacaoStuffUnstuff> getHistorico() { return Collections.unmodifiableList(historico); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getIniciadoEm() { return iniciadoEm; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
}
