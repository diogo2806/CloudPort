package br.com.cloudport.servicocargageral.dominio;

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
import javax.persistence.Version;

@Entity
@Table(name = "ordem_trabalho_carga")
public class OrdemTrabalhoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoServicoOrdemCarga tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemTrabalhoCarga status = StatusOrdemTrabalhoCarga.PLANEJADA;

    @Column(nullable = false)
    private Integer prioridade;

    @Column(name = "janela_inicio", nullable = false)
    private OffsetDateTime janelaInicio;

    @Column(name = "janela_fim", nullable = false)
    private OffsetDateTime janelaFim;

    @Column(nullable = false, length = 120)
    private String local;

    @Column(name = "equipe_id", length = 80)
    private String equipeId;

    @Column(name = "equipamento_id", length = 80)
    private String equipamentoId;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "liberado_em")
    private OffsetDateTime liberadoEm;

    @Column(name = "iniciado_em")
    private OffsetDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @Version
    private Long versao;

    @OneToMany(mappedBy = "ordem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemOrdemTrabalhoCarga> itens = new ArrayList<>();

    @OneToMany(mappedBy = "ordem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm ASC")
    private List<EventoOrdemTrabalhoCarga> eventos = new ArrayList<>();

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        numero = normalizar(numero);
        local = normalizar(local);
        validarJanela();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        numero = normalizar(numero);
        local = normalizar(local);
        validarJanela();
    }

    public void adicionarItem(ItemOrdemTrabalhoCarga item) {
        exigirStatus(StatusOrdemTrabalhoCarga.PLANEJADA);
        item.setOrdem(this);
        itens.add(item);
    }

    public void liberar(String usuario) {
        exigirStatus(StatusOrdemTrabalhoCarga.PLANEJADA);
        if (itens.isEmpty()) {
            throw new IllegalStateException("A ordem deve possuir ao menos um item para ser liberada.");
        }
        status = StatusOrdemTrabalhoCarga.LIBERADA;
        liberadoEm = OffsetDateTime.now();
        registrarEvento(TipoEventoOrdemCarga.LIBERADA, "Ordem liberada para execução.", usuario);
    }

    public void atribuirRecursos(String equipeId, String equipamentoId, String usuario) {
        if (status != StatusOrdemTrabalhoCarga.PLANEJADA && status != StatusOrdemTrabalhoCarga.LIBERADA) {
            throw new IllegalStateException("Recursos só podem ser atribuídos antes do início da execução.");
        }
        if (vazio(equipeId) && vazio(equipamentoId)) {
            throw new IllegalArgumentException("Informe equipe ou equipamento.");
        }
        this.equipeId = normalizarOpcional(equipeId);
        this.equipamentoId = normalizarOpcional(equipamentoId);
        registrarEvento(TipoEventoOrdemCarga.RECURSOS_ATRIBUIDOS, "Recursos operacionais atribuídos.", usuario);
    }

    public void iniciar(String usuario) {
        exigirStatus(StatusOrdemTrabalhoCarga.LIBERADA);
        status = StatusOrdemTrabalhoCarga.EM_EXECUCAO;
        iniciadoEm = OffsetDateTime.now();
        registrarEvento(TipoEventoOrdemCarga.INICIADA, "Execução iniciada.", usuario);
    }

    public void registrarServico(String descricao, String usuario) {
        exigirStatus(StatusOrdemTrabalhoCarga.EM_EXECUCAO);
        if (vazio(descricao)) {
            throw new IllegalArgumentException("A descrição do evento é obrigatória.");
        }
        registrarEvento(TipoEventoOrdemCarga.SERVICO_REGISTRADO, descricao.trim(), usuario);
    }

    public void concluir(String usuario) {
        exigirStatus(StatusOrdemTrabalhoCarga.EM_EXECUCAO);
        status = StatusOrdemTrabalhoCarga.CONCLUIDA;
        concluidoEm = OffsetDateTime.now();
        registrarEvento(TipoEventoOrdemCarga.CONCLUIDA, "Ordem concluída.", usuario);
    }

    public void cancelar(String motivo, String usuario) {
        if (status == StatusOrdemTrabalhoCarga.CONCLUIDA || status == StatusOrdemTrabalhoCarga.CANCELADA) {
            throw new IllegalStateException("Ordem em estado terminal não pode ser cancelada.");
        }
        if (vazio(motivo)) {
            throw new IllegalArgumentException("O motivo do cancelamento é obrigatório.");
        }
        motivoCancelamento = motivo.trim();
        status = StatusOrdemTrabalhoCarga.CANCELADA;
        canceladoEm = OffsetDateTime.now();
        registrarEvento(TipoEventoOrdemCarga.CANCELADA, motivoCancelamento, usuario);
    }

    public void registrarCriacao(String usuario) {
        registrarEvento(TipoEventoOrdemCarga.CRIADA, "Ordem planejada.", usuario);
    }

    private void registrarEvento(TipoEventoOrdemCarga tipo, String descricao, String usuario) {
        EventoOrdemTrabalhoCarga evento = new EventoOrdemTrabalhoCarga();
        evento.setOrdem(this);
        evento.setTipo(tipo);
        evento.setDescricao(descricao);
        evento.setUsuario(normalizarUsuario(usuario));
        eventos.add(evento);
    }

    private void exigirStatus(StatusOrdemTrabalhoCarga esperado) {
        if (status != esperado) {
            throw new IllegalStateException("Transição inválida para ordem no estado " + status + ".");
        }
    }

    private void validarJanela() {
        if (janelaInicio == null || janelaFim == null || !janelaFim.isAfter(janelaInicio)) {
            throw new IllegalStateException("A janela operacional deve possuir fim posterior ao início.");
        }
    }

    private String normalizar(String valor) {
        if (vazio(valor)) {
            throw new IllegalStateException("Campo obrigatório não informado.");
        }
        return valor.trim().toUpperCase();
    }

    private String normalizarOpcional(String valor) {
        return vazio(valor) ? null : valor.trim().toUpperCase();
    }

    private String normalizarUsuario(String usuario) {
        return vazio(usuario) ? "SISTEMA" : usuario.trim();
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public UUID getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public TipoServicoOrdemCarga getTipo() { return tipo; }
    public void setTipo(TipoServicoOrdemCarga tipo) { this.tipo = tipo; }
    public StatusOrdemTrabalhoCarga getStatus() { return status; }
    public Integer getPrioridade() { return prioridade; }
    public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
    public OffsetDateTime getJanelaInicio() { return janelaInicio; }
    public void setJanelaInicio(OffsetDateTime janelaInicio) { this.janelaInicio = janelaInicio; }
    public OffsetDateTime getJanelaFim() { return janelaFim; }
    public void setJanelaFim(OffsetDateTime janelaFim) { this.janelaFim = janelaFim; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getEquipeId() { return equipeId; }
    public String getEquipamentoId() { return equipamentoId; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public OffsetDateTime getLiberadoEm() { return liberadoEm; }
    public OffsetDateTime getIniciadoEm() { return iniciadoEm; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
    public Long getVersao() { return versao; }
    public List<ItemOrdemTrabalhoCarga> getItens() { return Collections.unmodifiableList(itens); }
    public List<EventoOrdemTrabalhoCarga> getEventos() { return Collections.unmodifiableList(eventos); }
}
