package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusPlanoOperacional;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.TipoUnidadeIntermodal;
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
@Table(name = "plano_operacional_carga")
public class PlanoOperacionalCarga {

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
    private StatusPlanoOperacional status = StatusPlanoOperacional.RASCUNHO;

    @Column(nullable = false)
    private Integer prioridade;

    @Column(name = "versao_plano", nullable = false)
    private Integer versaoPlano = 1;

    @Column(name = "plano_origem_id")
    private UUID planoOrigemId;

    @Column(name = "janela_inicio", nullable = false)
    private OffsetDateTime janelaInicio;

    @Column(name = "janela_fim", nullable = false)
    private OffsetDateTime janelaFim;

    @Column(nullable = false, length = 120)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_tipo", length = 30)
    private TipoUnidadeIntermodal origemTipo;

    @Column(name = "origem_id", length = 120)
    private String origemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destino_tipo", length = 30)
    private TipoUnidadeIntermodal destinoTipo;

    @Column(name = "destino_id", length = 120)
    private String destinoId;

    @Column(name = "visita_navio_id", length = 80)
    private String visitaNavioId;

    @Column(name = "visita_ferroviaria_id", length = 80)
    private String visitaFerroviariaId;

    @Column(name = "equipe_id", length = 80)
    private String equipeId;

    @Column(name = "equipamento_id", length = 80)
    private String equipamentoId;

    @Column(name = "lacre_origem", length = 80)
    private String lacreOrigem;

    @Column(name = "lacre_destino", length = 80)
    private String lacreDestino;

    @Column(length = 2000)
    private String restricoes;

    @Column(name = "instrucao_trabalho", length = 2000)
    private String instrucaoTrabalho;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @Column(name = "historico_operacional", length = 16000)
    private String historicoOperacional = "";

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

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequencia ASC")
    private List<ItemPlanoOperacionalCarga> itens = new ArrayList<>();

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        normalizar();
        validarJanela();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        normalizar();
        validarJanela();
    }

    public void adicionarItem(ItemPlanoOperacionalCarga item) {
        exigirStatus(StatusPlanoOperacional.RASCUNHO);
        item.setPlano(this);
        itens.add(item);
    }

    public void liberar(String usuario) {
        exigirStatus(StatusPlanoOperacional.RASCUNHO);
        if (itens.isEmpty()) {
            throw new IllegalStateException("O plano deve possuir ao menos um cargo lot.");
        }
        status = StatusPlanoOperacional.LIBERADO;
        liberadoEm = OffsetDateTime.now();
        registrarHistorico("LIBERADO", usuario, "Plano liberado para execução.");
    }

    public void atribuirRecursos(String equipe, String equipamento, String usuario) {
        if (status != StatusPlanoOperacional.RASCUNHO && status != StatusPlanoOperacional.LIBERADO) {
            throw new IllegalStateException("Os recursos devem ser atribuídos antes do início.");
        }
        if (vazio(equipe) && vazio(equipamento)) {
            throw new IllegalArgumentException("Informe equipe ou equipamento.");
        }
        equipeId = opcional(equipe);
        equipamentoId = opcional(equipamento);
        registrarHistorico("RECURSOS", usuario, "Equipe e equipamento atribuídos.");
    }

    public void iniciar(String usuario) {
        exigirStatus(StatusPlanoOperacional.LIBERADO);
        status = StatusPlanoOperacional.EM_EXECUCAO;
        iniciadoEm = OffsetDateTime.now();
        registrarHistorico("INICIADO", usuario, "Execução física iniciada.");
    }

    public void atualizarStatusExecucao() {
        boolean executado = itens.stream().anyMatch(ItemPlanoOperacionalCarga::possuiExecucao);
        boolean completo = itens.stream().allMatch(ItemPlanoOperacionalCarga::estaCompleto);
        if (completo) {
            status = StatusPlanoOperacional.EM_EXECUCAO;
        } else if (executado) {
            status = StatusPlanoOperacional.PARCIAL;
        }
    }

    public void concluir(boolean aceitarDivergencia, String usuario, String observacao) {
        if (status != StatusPlanoOperacional.EM_EXECUCAO && status != StatusPlanoOperacional.PARCIAL) {
            throw new IllegalStateException("Somente plano iniciado pode ser concluído.");
        }
        boolean incompleto = itens.stream().anyMatch(item -> !item.estaCompleto());
        if (incompleto && !aceitarDivergencia) {
            throw new IllegalStateException("Existem quantidades planejadas ainda não executadas.");
        }
        status = StatusPlanoOperacional.CONCLUIDO;
        concluidoEm = OffsetDateTime.now();
        registrarHistorico("CONCLUIDO", usuario, observacao == null ? "Plano reconciliado." : observacao);
    }

    public void cancelar(String motivo, String usuario) {
        if (status == StatusPlanoOperacional.CONCLUIDO || status == StatusPlanoOperacional.CANCELADO) {
            throw new IllegalStateException("Plano em estado terminal não pode ser cancelado.");
        }
        if (vazio(motivo)) {
            throw new IllegalArgumentException("O motivo do cancelamento é obrigatório.");
        }
        motivoCancelamento = motivo.trim();
        status = StatusPlanoOperacional.CANCELADO;
        canceladoEm = OffsetDateTime.now();
        registrarHistorico("CANCELADO", usuario, motivoCancelamento);
    }

    public void registrarHistorico(String evento, String usuario, String descricao) {
        String linha = OffsetDateTime.now() + "|" + normalizarUsuario(usuario) + "|" + evento + "|"
                + (descricao == null ? "" : descricao.trim());
        historicoOperacional = historicoOperacional == null || historicoOperacional.isBlank()
                ? linha : historicoOperacional + "\n" + linha;
    }

    private void exigirStatus(StatusPlanoOperacional esperado) {
        if (status != esperado) {
            throw new IllegalStateException("Transição inválida para plano no estado " + status + ".");
        }
    }

    private void validarJanela() {
        if (janelaInicio == null || janelaFim == null || !janelaFim.isAfter(janelaInicio)) {
            throw new IllegalStateException("A janela operacional deve possuir fim posterior ao início.");
        }
    }

    private void normalizar() {
        numero = obrigatorio(numero);
        local = obrigatorio(local);
        origemId = opcional(origemId);
        destinoId = opcional(destinoId);
        visitaNavioId = opcional(visitaNavioId);
        visitaFerroviariaId = opcional(visitaFerroviariaId);
        lacreOrigem = opcional(lacreOrigem);
        lacreDestino = opcional(lacreDestino);
    }

    private String obrigatorio(String valor) {
        if (vazio(valor)) {
            throw new IllegalStateException("Campo obrigatório não informado.");
        }
        return valor.trim().toUpperCase();
    }

    private String opcional(String valor) {
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
    public StatusPlanoOperacional getStatus() { return status; }
    public Integer getPrioridade() { return prioridade; }
    public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
    public Integer getVersaoPlano() { return versaoPlano; }
    public void setVersaoPlano(Integer versaoPlano) { this.versaoPlano = versaoPlano; }
    public UUID getPlanoOrigemId() { return planoOrigemId; }
    public void setPlanoOrigemId(UUID planoOrigemId) { this.planoOrigemId = planoOrigemId; }
    public OffsetDateTime getJanelaInicio() { return janelaInicio; }
    public void setJanelaInicio(OffsetDateTime janelaInicio) { this.janelaInicio = janelaInicio; }
    public OffsetDateTime getJanelaFim() { return janelaFim; }
    public void setJanelaFim(OffsetDateTime janelaFim) { this.janelaFim = janelaFim; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public TipoUnidadeIntermodal getOrigemTipo() { return origemTipo; }
    public void setOrigemTipo(TipoUnidadeIntermodal origemTipo) { this.origemTipo = origemTipo; }
    public String getOrigemId() { return origemId; }
    public void setOrigemId(String origemId) { this.origemId = origemId; }
    public TipoUnidadeIntermodal getDestinoTipo() { return destinoTipo; }
    public void setDestinoTipo(TipoUnidadeIntermodal destinoTipo) { this.destinoTipo = destinoTipo; }
    public String getDestinoId() { return destinoId; }
    public void setDestinoId(String destinoId) { this.destinoId = destinoId; }
    public String getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(String visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getVisitaFerroviariaId() { return visitaFerroviariaId; }
    public void setVisitaFerroviariaId(String visitaFerroviariaId) { this.visitaFerroviariaId = visitaFerroviariaId; }
    public String getEquipeId() { return equipeId; }
    public String getEquipamentoId() { return equipamentoId; }
    public String getLacreOrigem() { return lacreOrigem; }
    public void setLacreOrigem(String lacreOrigem) { this.lacreOrigem = lacreOrigem; }
    public String getLacreDestino() { return lacreDestino; }
    public void setLacreDestino(String lacreDestino) { this.lacreDestino = lacreDestino; }
    public String getRestricoes() { return restricoes; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public String getInstrucaoTrabalho() { return instrucaoTrabalho; }
    public void setInstrucaoTrabalho(String instrucaoTrabalho) { this.instrucaoTrabalho = instrucaoTrabalho; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public String getHistoricoOperacional() { return historicoOperacional; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public OffsetDateTime getLiberadoEm() { return liberadoEm; }
    public OffsetDateTime getIniciadoEm() { return iniciadoEm; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
    public Long getVersao() { return versao; }
    public List<ItemPlanoOperacionalCarga> getItens() { return Collections.unmodifiableList(itens); }
}
