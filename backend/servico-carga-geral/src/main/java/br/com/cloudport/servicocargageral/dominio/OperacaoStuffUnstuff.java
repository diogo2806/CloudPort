package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.MetodoPesagemVgm;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusPesagemVgm;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import java.math.BigDecimal;
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

    private static final BigDecimal TOLERANCIA_PESAGEM_KG = new BigDecimal("1.000");
    private static final int LIMITE_DESCRICAO_EVENTO = 1000;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pesagem_vgm", length = 20)
    private MetodoPesagemVgm metodoPesagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pesagem_vgm", nullable = false, length = 30)
    private StatusPesagemVgm statusPesagemVgm = StatusPesagemVgm.PENDENTE;

    @Column(name = "tara_kg", precision = 19, scale = 3)
    private BigDecimal taraKg;

    @Column(name = "peso_bruto_kg", precision = 19, scale = 3)
    private BigDecimal pesoBrutoKg;

    @Column(name = "vgm_kg", precision = 19, scale = 3)
    private BigDecimal vgmKg;

    @Column(name = "capacidade_maxima_kg", precision = 19, scale = 3)
    private BigDecimal capacidadeMaximaKg;

    @Column(name = "equipamento_pesagem", length = 120)
    private String equipamentoPesagem;

    @Column(name = "responsavel_pesagem", length = 120)
    private String responsavelPesagem;

    @Column(name = "pesagem_confirmada_em")
    private OffsetDateTime pesagemConfirmadaEm;

    @Column(name = "motivo_bloqueio_peso", length = 1000)
    private String motivoBloqueioPeso;

    @OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("criadoEm ASC")
    private List<ItemOperacaoStuffUnstuff> itens = new ArrayList<>();

    @OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm ASC")
    private List<EventoOperacaoStuffUnstuff> historico = new ArrayList<>();

    @OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm ASC")
    private List<LacreOperacaoStuffUnstuff> lacres = new ArrayList<>();

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
        lacreInicial = normalizar(lacreInicial);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        conteinerId = normalizar(conteinerId);
        lacreInicial = normalizar(lacreInicial);
        lacreFinal = normalizar(lacreFinal);
        equipamentoPesagem = normalizar(equipamentoPesagem);
        responsavelPesagem = normalizarTexto(responsavelPesagem);
    }

    public void adicionarItem(ItemOperacaoStuffUnstuff item) {
        item.setOperacao(this);
        itens.add(item);
    }

    public void substituirItens(List<ItemOperacaoStuffUnstuff> novosItens) {
        if (status != StatusOperacaoStuffUnstuff.PLANEJADA || possuiExecucao()) {
            throw new IllegalStateException("Itens só podem ser substituídos antes da execução física.");
        }
        itens.clear();
        novosItens.forEach(this::adicionarItem);
    }

    public boolean possuiExecucao() {
        return itens.stream().anyMatch(ItemOperacaoStuffUnstuff::possuiExecucao);
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

    public void adicionarLacre(LacreOperacaoStuffUnstuff lacre) {
        lacre.setOperacao(this);
        lacres.add(lacre);
    }

    public boolean possuiDivergenciaLacreAberta() {
        return lacres.stream().anyMatch(lacre -> lacre.isDivergenciaAberta() && !lacre.isOverrideAutorizado());
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

    public void confirmarPesagemStuffing(
            MetodoPesagemVgm metodo,
            BigDecimal tara,
            BigDecimal pesoBruto,
            BigDecimal vgm,
            BigDecimal capacidadeMaxima,
            String equipamento,
            String responsavel,
            String usuario,
            String correlationId,
            String observacao) {
        if (tipo != TipoOperacaoStuffUnstuff.STUFF) {
            throw new IllegalStateException("A confirmação de VGM é permitida somente para operação de stuffing.");
        }
        if (status == StatusOperacaoStuffUnstuff.CANCELADA || status == StatusOperacaoStuffUnstuff.CONCLUIDA) {
            throw new IllegalStateException("Operação encerrada não aceita confirmação de pesagem.");
        }
        if (itens.stream().anyMatch(item -> !item.estaCompleto())) {
            throw new IllegalStateException("Todos os itens devem estar integralmente executados antes da pesagem.");
        }
        validarValorPositivo(tara, "Tara");
        validarValorPositivo(pesoBruto, "Peso bruto");
        validarValorPositivo(vgm, "VGM");
        validarValorPositivo(capacidadeMaxima, "Capacidade máxima");
        if (metodo == null) {
            throw new IllegalStateException("Método de pesagem deve ser informado.");
        }
        if (equipamento == null || equipamento.isBlank()) {
            throw new IllegalStateException("Equipamento de pesagem deve ser informado.");
        }
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalStateException("Responsável pela pesagem deve ser informado.");
        }
        if (pesoBruto.compareTo(tara) < 0) {
            throw new IllegalStateException("Peso bruto não pode ser menor que a tara do contêiner.");
        }
        validarDiferencaMaxima(pesoBruto, vgm, "Peso bruto e VGM divergem acima da tolerância de 1 kg.");
        if (metodo == MetodoPesagemVgm.METODO_2) {
            BigDecimal pesoCarga = itens.stream()
                    .map(ItemOperacaoStuffUnstuff::getPesoRealizadoKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            validarDiferencaMaxima(tara.add(pesoCarga), vgm,
                    "No método 2, o VGM deve corresponder à tara somada ao peso executado da carga.");
        }

        metodoPesagem = metodo;
        taraKg = tara;
        pesoBrutoKg = pesoBruto;
        vgmKg = vgm;
        capacidadeMaximaKg = capacidadeMaxima;
        equipamentoPesagem = normalizar(equipamento);
        responsavelPesagem = normalizarTexto(responsavel);
        pesagemConfirmadaEm = OffsetDateTime.now();

        if (vgm.compareTo(capacidadeMaxima) > 0) {
            statusPesagemVgm = StatusPesagemVgm.BLOQUEADA_EXCESSO;
            motivoBloqueioPeso = "VGM de " + vgm.toPlainString() + " kg excede a capacidade máxima de "
                    + capacidadeMaxima.toPlainString() + " kg.";
            registrarEvento(TipoEventoStuffUnstuff.PESAGEM_BLOQUEADA, usuario, correlationId,
                    montarDescricaoPesagem(observacao, motivoBloqueioPeso));
            return;
        }

        statusPesagemVgm = StatusPesagemVgm.CONFIRMADA;
        motivoBloqueioPeso = null;
        registrarEvento(TipoEventoStuffUnstuff.PESAGEM_CONFIRMADA, usuario, correlationId,
                montarDescricaoPesagem(observacao, "Pesagem confirmada e contêiner liberado por peso."));
    }

    public boolean possuiPesagemLiberada() {
        return tipo != TipoOperacaoStuffUnstuff.STUFF || statusPesagemVgm == StatusPesagemVgm.CONFIRMADA;
    }

    public void concluir(String lacre, String observacao, String usuario, String correlationId) {
        if (status == StatusOperacaoStuffUnstuff.CANCELADA || status == StatusOperacaoStuffUnstuff.CONCLUIDA) {
            throw new IllegalStateException("Operação já está encerrada.");
        }
        if (itens.stream().anyMatch(item -> !item.estaCompleto())) {
            throw new IllegalStateException("Todos os itens devem atingir a quantidade planejada antes da conclusão.");
        }
        if (possuiDivergenciaLacreAberta()) {
            throw new IllegalStateException("A operação possui divergência de lacre aberta sem override autorizado.");
        }
        if (!possuiPesagemLiberada()) {
            String detalhe = motivoBloqueioPeso == null ? "A pesagem e o VGM ainda não foram confirmados." : motivoBloqueioPeso;
            throw new IllegalStateException("O stuffing não pode ser concluído: " + detalhe);
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

    private void validarValorPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(campo + " deve ser maior que zero.");
        }
    }

    private void validarDiferencaMaxima(BigDecimal esperado, BigDecimal informado, String mensagem) {
        if (esperado.subtract(informado).abs().compareTo(TOLERANCIA_PESAGEM_KG) > 0) {
            throw new IllegalStateException(mensagem);
        }
    }

    private String montarDescricaoPesagem(String observacao, String resultado) {
        String detalhes = "Método " + metodoPesagem + ", tara " + taraKg.toPlainString()
                + " kg, peso bruto/VGM " + vgmKg.toPlainString() + " kg, equipamento "
                + equipamentoPesagem + ", responsável " + responsavelPesagem + ". ";
        String descricao = detalhes + resultado
                + (observacao == null || observacao.isBlank() ? "" : " " + observacao.trim());
        return descricao.length() <= LIMITE_DESCRICAO_EVENTO
                ? descricao
                : descricao.substring(0, LIMITE_DESCRICAO_EVENTO);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.trim();
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
    public MetodoPesagemVgm getMetodoPesagem() { return metodoPesagem; }
    public StatusPesagemVgm getStatusPesagemVgm() { return statusPesagemVgm; }
    public BigDecimal getTaraKg() { return taraKg; }
    public BigDecimal getPesoBrutoKg() { return pesoBrutoKg; }
    public BigDecimal getVgmKg() { return vgmKg; }
    public BigDecimal getCapacidadeMaximaKg() { return capacidadeMaximaKg; }
    public String getEquipamentoPesagem() { return equipamentoPesagem; }
    public String getResponsavelPesagem() { return responsavelPesagem; }
    public OffsetDateTime getPesagemConfirmadaEm() { return pesagemConfirmadaEm; }
    public String getMotivoBloqueioPeso() { return motivoBloqueioPeso; }
    public List<ItemOperacaoStuffUnstuff> getItens() { return Collections.unmodifiableList(itens); }
    public List<EventoOperacaoStuffUnstuff> getHistorico() { return Collections.unmodifiableList(historico); }
    public List<LacreOperacaoStuffUnstuff> getLacres() { return Collections.unmodifiableList(lacres); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getIniciadoEm() { return iniciadoEm; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
}
