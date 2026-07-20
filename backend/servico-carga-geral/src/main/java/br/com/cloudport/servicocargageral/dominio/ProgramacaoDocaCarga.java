package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusProgramacaoDocaCarga;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "programacao_doca_carga")
public class ProgramacaoDocaCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id", nullable = false, unique = true)
    private OperacaoStuffUnstuff operacao;

    @Column(name = "conteiner_id", nullable = false, length = 80)
    private String conteinerId;

    @Column(name = "doca_id", nullable = false, length = 80)
    private String docaId;

    @Column(name = "area_espera_id", nullable = false, length = 80)
    private String areaEsperaId;

    @Column(name = "recurso_id", nullable = false, length = 120)
    private String recursoId;

    @Column(name = "janela_inicio", nullable = false)
    private OffsetDateTime janelaInicio;

    @Column(name = "janela_fim", nullable = false)
    private OffsetDateTime janelaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProgramacaoDocaCarga status = StatusProgramacaoDocaCarga.RESERVADA;

    @Column(name = "reservado_por", nullable = false, length = 120)
    private String reservadoPor;

    @Column(name = "reservado_em", nullable = false)
    private OffsetDateTime reservadoEm;

    @Column(name = "observacao_reserva", length = 1000)
    private String observacaoReserva;

    @Column(name = "iniciado_por", length = 120)
    private String iniciadoPor;

    @Column(name = "iniciado_em")
    private OffsetDateTime iniciadoEm;

    @Column(name = "concluido_por", length = 120)
    private String concluidoPor;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    @Column(name = "cancelado_por", length = 120)
    private String canceladoPor;

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "programacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("loteCodigo ASC")
    private List<ReservaLoteProgramacaoDocaCarga> reservasLote = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long versao;

    @PrePersist
    void prePersist() {
        reservadoEm = reservadoEm == null ? OffsetDateTime.now() : reservadoEm;
        normalizarCampos();
        validarPeriodo(janelaInicio, janelaFim);
    }

    @PreUpdate
    void preUpdate() {
        normalizarCampos();
        validarPeriodo(janelaInicio, janelaFim);
    }

    public void reservar(
            OperacaoStuffUnstuff operacao,
            String doca,
            String areaEspera,
            String recurso,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            String usuario,
            String observacao) {
        if (status == StatusProgramacaoDocaCarga.EM_USO || status == StatusProgramacaoDocaCarga.CONCLUIDA) {
            throw new IllegalStateException("A programação em uso ou concluída não pode ser alterada.");
        }
        validarTexto(doca, "Doca");
        validarTexto(areaEspera, "Área de espera");
        validarTexto(recurso, "Recurso operacional");
        validarTexto(usuario, "Usuário da reserva");
        validarPeriodo(inicio, fim);
        if (!fim.isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("A janela operacional deve terminar no futuro.");
        }

        this.operacao = operacao;
        conteinerId = normalizar(operacao.getConteinerId());
        docaId = normalizar(doca);
        areaEsperaId = normalizar(areaEspera);
        recursoId = normalizar(recurso);
        janelaInicio = inicio;
        janelaFim = fim;
        reservadoPor = normalizarTexto(usuario);
        reservadoEm = OffsetDateTime.now();
        observacaoReserva = normalizarTextoLongo(observacao);
        status = StatusProgramacaoDocaCarga.RESERVADA;
        iniciadoPor = null;
        iniciadoEm = null;
        concluidoPor = null;
        concluidoEm = null;
        canceladoPor = null;
        canceladoEm = null;
        motivoCancelamento = null;
        sincronizarReservasLote(operacao.getItens());
    }

    public void iniciar(String usuario) {
        if (status != StatusProgramacaoDocaCarga.RESERVADA) {
            throw new IllegalStateException("Somente uma programação reservada pode iniciar a ocupação operacional.");
        }
        validarTexto(usuario, "Usuário do início");
        OffsetDateTime agora = OffsetDateTime.now();
        if (agora.isBefore(janelaInicio)) {
            throw new IllegalStateException("A janela operacional ainda não foi iniciada.");
        }
        if (!agora.isBefore(janelaFim)) {
            throw new IllegalStateException("A janela operacional expirou e deve ser reprogramada.");
        }
        status = StatusProgramacaoDocaCarga.EM_USO;
        iniciadoPor = normalizarTexto(usuario);
        iniciadoEm = agora;
        reservasLote.forEach(reserva -> reserva.alterarStatus(StatusProgramacaoDocaCarga.EM_USO));
    }

    public void concluir(String usuario) {
        if (status != StatusProgramacaoDocaCarga.EM_USO) {
            throw new IllegalStateException("Somente uma programação em uso pode ser concluída.");
        }
        validarTexto(usuario, "Usuário da conclusão");
        status = StatusProgramacaoDocaCarga.CONCLUIDA;
        concluidoPor = normalizarTexto(usuario);
        concluidoEm = OffsetDateTime.now();
        reservasLote.forEach(reserva -> reserva.alterarStatus(StatusProgramacaoDocaCarga.CONCLUIDA));
    }

    public void cancelar(String usuario, String motivo, boolean permitirEmUso) {
        if (status == StatusProgramacaoDocaCarga.CONCLUIDA || status == StatusProgramacaoDocaCarga.CANCELADA) {
            throw new IllegalStateException("A programação já está encerrada.");
        }
        if (status == StatusProgramacaoDocaCarga.EM_USO && !permitirEmUso) {
            throw new IllegalStateException("A programação em uso só pode ser liberada pelo cancelamento da operação.");
        }
        validarTexto(usuario, "Usuário do cancelamento");
        validarTexto(motivo, "Motivo do cancelamento");
        status = StatusProgramacaoDocaCarga.CANCELADA;
        canceladoPor = normalizarTexto(usuario);
        canceladoEm = OffsetDateTime.now();
        motivoCancelamento = normalizarTextoLongo(motivo);
        reservasLote.forEach(reserva -> reserva.alterarStatus(StatusProgramacaoDocaCarga.CANCELADA));
    }

    public boolean ocupaRecursos() {
        return status == StatusProgramacaoDocaCarga.RESERVADA || status == StatusProgramacaoDocaCarga.EM_USO;
    }

    private void sincronizarReservasLote(List<ItemOperacaoStuffUnstuff> itens) {
        Map<UUID, ReservaLoteProgramacaoDocaCarga> existentes = new HashMap<>();
        reservasLote.forEach(reserva -> existentes.put(reserva.getLoteId(), reserva));
        List<ReservaLoteProgramacaoDocaCarga> sincronizadas = new ArrayList<>();
        for (ItemOperacaoStuffUnstuff item : itens) {
            UUID loteId = item.getLote().getId();
            ReservaLoteProgramacaoDocaCarga reserva = existentes.remove(loteId);
            if (reserva == null) {
                reserva = new ReservaLoteProgramacaoDocaCarga();
                reserva.setProgramacao(this);
            }
            reserva.sincronizar(item, janelaInicio, janelaFim, status);
            sincronizadas.add(reserva);
        }
        reservasLote.clear();
        reservasLote.addAll(sincronizadas);
    }

    private void validarPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        if (inicio == null || fim == null) {
            throw new IllegalStateException("Início e fim da janela operacional devem ser informados.");
        }
        if (!fim.isAfter(inicio)) {
            throw new IllegalStateException("O fim da janela operacional deve ser posterior ao início.");
        }
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(campo + " deve ser informado.");
        }
    }

    private void normalizarCampos() {
        conteinerId = normalizar(conteinerId);
        docaId = normalizar(docaId);
        areaEsperaId = normalizar(areaEsperaId);
        recursoId = normalizar(recursoId);
        reservadoPor = normalizarTexto(reservadoPor);
        observacaoReserva = normalizarTextoLongo(observacaoReserva);
        iniciadoPor = normalizarTexto(iniciadoPor);
        concluidoPor = normalizarTexto(concluidoPor);
        canceladoPor = normalizarTexto(canceladoPor);
        motivoCancelamento = normalizarTextoLongo(motivoCancelamento);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String normalizarTextoLongo(String valor) {
        String normalizado = normalizarTexto(valor);
        if (normalizado == null || normalizado.length() <= 1000) {
            return normalizado;
        }
        return normalizado.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public OperacaoStuffUnstuff getOperacao() {
        return operacao;
    }

    public String getConteinerId() {
        return conteinerId;
    }

    public String getDocaId() {
        return docaId;
    }

    public String getAreaEsperaId() {
        return areaEsperaId;
    }

    public String getRecursoId() {
        return recursoId;
    }

    public OffsetDateTime getJanelaInicio() {
        return janelaInicio;
    }

    public OffsetDateTime getJanelaFim() {
        return janelaFim;
    }

    public StatusProgramacaoDocaCarga getStatus() {
        return status;
    }

    public String getReservadoPor() {
        return reservadoPor;
    }

    public OffsetDateTime getReservadoEm() {
        return reservadoEm;
    }

    public String getObservacaoReserva() {
        return observacaoReserva;
    }

    public String getIniciadoPor() {
        return iniciadoPor;
    }

    public OffsetDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public String getConcluidoPor() {
        return concluidoPor;
    }

    public OffsetDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public OffsetDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<ReservaLoteProgramacaoDocaCarga> getReservasLote() {
        return Collections.unmodifiableList(reservasLote);
    }

    public long getVersao() {
        return versao;
    }
}
