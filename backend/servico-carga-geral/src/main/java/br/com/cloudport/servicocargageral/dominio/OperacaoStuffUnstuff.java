package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
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
import javax.persistence.Version;

@Entity
@Table(name = "operacao_stuff_unstuff")
public class OperacaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOperacaoStuffUnstuff tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOperacaoStuffUnstuff status = StatusOperacaoStuffUnstuff.PLANEJADA;

    @Column(name = "container_id", nullable = false, length = 80)
    private String containerId;

    @Column(name = "armazem_id", length = 80)
    private String armazemId;

    @Column(name = "posicao_operacao", length = 120)
    private String posicaoOperacao;

    @Column(name = "equipe_recurso", length = 160)
    private String equipeRecurso;

    @Column(name = "lacre_inicial", length = 80)
    private String lacreInicial;

    @Column(name = "lacre_final", length = 80)
    private String lacreFinal;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("criadoEm ASC")
    private List<ItemOperacaoStuffUnstuff> itens = new ArrayList<>();

    @Column(name = "iniciada_em")
    private OffsetDateTime iniciadaEm;

    @Column(name = "concluida_em")
    private OffsetDateTime concluidaEm;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    private long versao;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        numero = normalizar(numero);
        containerId = normalizar(containerId);
        usuario = usuario.trim();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }

    public void adicionarItem(ItemOperacaoStuffUnstuff item) {
        item.setOperacao(this);
        itens.add(item);
    }

    public void iniciar() {
        exigirStatus(StatusOperacaoStuffUnstuff.PLANEJADA);
        status = StatusOperacaoStuffUnstuff.EM_EXECUCAO;
        iniciadaEm = OffsetDateTime.now();
    }

    public void marcarParcial() {
        if (status != StatusOperacaoStuffUnstuff.EM_EXECUCAO && status != StatusOperacaoStuffUnstuff.PARCIAL) {
            throw new IllegalStateException("A operação não está disponível para execução.");
        }
        status = StatusOperacaoStuffUnstuff.PARCIAL;
    }

    public void concluir(String lacreFinal) {
        if (status != StatusOperacaoStuffUnstuff.EM_EXECUCAO && status != StatusOperacaoStuffUnstuff.PARCIAL) {
            throw new IllegalStateException("A operação não está disponível para conclusão.");
        }
        if (itens.stream().anyMatch(item -> !item.podeConcluir())) {
            throw new IllegalStateException("Todos os itens devem possuir execução e divergências justificadas antes da conclusão.");
        }
        this.lacreFinal = normalizar(lacreFinal);
        status = StatusOperacaoStuffUnstuff.CONCLUIDA;
        concluidaEm = OffsetDateTime.now();
    }

    public void cancelar(String motivo) {
        if (status == StatusOperacaoStuffUnstuff.CONCLUIDA || status == StatusOperacaoStuffUnstuff.CANCELADA) {
            throw new IllegalStateException("Operação concluída ou cancelada não pode ser cancelada novamente.");
        }
        motivoCancelamento = motivo.trim();
        status = StatusOperacaoStuffUnstuff.CANCELADA;
        canceladaEm = OffsetDateTime.now();
    }

    private void exigirStatus(StatusOperacaoStuffUnstuff esperado) {
        if (status != esperado) {
            throw new IllegalStateException("Status atual da operação não permite esta ação.");
        }
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public TipoOperacaoStuffUnstuff getTipo() { return tipo; }
    public void setTipo(TipoOperacaoStuffUnstuff tipo) { this.tipo = tipo; }
    public StatusOperacaoStuffUnstuff getStatus() { return status; }
    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getPosicaoOperacao() { return posicaoOperacao; }
    public void setPosicaoOperacao(String posicaoOperacao) { this.posicaoOperacao = posicaoOperacao; }
    public String getEquipeRecurso() { return equipeRecurso; }
    public void setEquipeRecurso(String equipeRecurso) { this.equipeRecurso = equipeRecurso; }
    public String getLacreInicial() { return lacreInicial; }
    public void setLacreInicial(String lacreInicial) { this.lacreInicial = lacreInicial; }
    public String getLacreFinal() { return lacreFinal; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public List<ItemOperacaoStuffUnstuff> getItens() { return Collections.unmodifiableList(itens); }
    public OffsetDateTime getIniciadaEm() { return iniciadaEm; }
    public OffsetDateTime getConcluidaEm() { return concluidaEm; }
    public OffsetDateTime getCanceladaEm() { return canceladaEm; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
