package br.com.cloudport.servicoyard.inventario.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "unidade_inventario")
public class UnidadeInventario {

    public enum EstadoUnidade {
        PRE_AVISADA,
        ATIVA,
        NO_PATIO,
        EM_OPERACAO,
        EM_TRANSITO,
        EMBARCADA,
        DESEMBARCADA,
        LIBERADA,
        DESPACHADA,
        INATIVA,
        APOSENTADA
    }

    public enum CondicaoEquipamento {
        OPERACIONAL,
        AVARIADO,
        INOPERANTE,
        EM_INSPECAO,
        EM_REPARO,
        AGUARDANDO_PECA
    }

    public enum StatusManutencao {
        NAO_REQUERIDA,
        ABERTA,
        EM_EXECUCAO,
        SUSPENSA,
        CONCLUIDA
    }

    public enum TipoRestricao {
        HOLD,
        PERMISSION
    }

    public enum StatusDocumento {
        PENDENTE,
        VALIDO,
        EXPIRADO,
        CANCELADO
    }

    public enum StatusAvaria {
        ABERTA,
        EM_REPARO,
        REPARADA,
        ACEITA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificacao", nullable = false, length = 40, unique = true)
    private String identificacao;

    @Column(name = "prefixo", length = 12)
    private String prefixo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_equipamento_id", nullable = false)
    private TipoEquipamentoInventario tipoEquipamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private TipoEquipamentoInventario.CategoriaEquipamento categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_unidade", nullable = false, length = 30)
    private EstadoUnidade estado = EstadoUnidade.PRE_AVISADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_equipamento", nullable = false, length = 30)
    private CondicaoEquipamento condicao = CondicaoEquipamento.OPERACIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_manutencao", nullable = false, length = 30)
    private StatusManutencao statusManutencao = StatusManutencao.NAO_REQUERIDA;

    @Column(name = "proprietario", length = 120)
    private String proprietario;

    @Column(name = "operador", length = 120)
    private String operador;

    @Column(name = "posicao_atual", length = 120)
    private String posicaoAtual;

    @Column(name = "posicao_planejada", length = 120)
    private String posicaoPlanejada;

    @Column(name = "peso_bruto_kg", precision = 14, scale = 3)
    private BigDecimal pesoBrutoKg;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_lacre", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<LacreRegistro> lacres = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_documento", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<DocumentoRegistro> documentos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_avaria", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<AvariaRegistro> avarias = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_restricao", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<RestricaoRegistro> restricoes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_manutencao", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<ManutencaoRegistro> manutencoes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_historico_atributo", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<HistoricoAtributoRegistro> historicoAtributos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "unidade_reefer_registro", joinColumns = @JoinColumn(name = "unidade_id"))
    @OrderColumn(name = "ordem")
    private List<ReeferRegistro> registrosReefer = new ArrayList<>();

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public boolean possuiHoldAtivo(LocalDateTime referencia) {
        return restricoes.stream().anyMatch(restricao -> restricao.getTipo() == TipoRestricao.HOLD
                && restricao.isAtiva()
                && (restricao.getValidoDe() == null || !restricao.getValidoDe().isAfter(referencia))
                && (restricao.getValidoAte() == null || !restricao.getValidoAte().isBefore(referencia)));
    }

    public boolean possuiPermissionAtiva(String codigo, LocalDateTime referencia) {
        return restricoes.stream().anyMatch(restricao -> restricao.getTipo() == TipoRestricao.PERMISSION
                && restricao.isAtiva()
                && (codigo == null || codigo.equalsIgnoreCase(restricao.getCodigo()))
                && (restricao.getValidoDe() == null || !restricao.getValidoDe().isAfter(referencia))
                && (restricao.getValidoAte() == null || !restricao.getValidoAte().isBefore(referencia)));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    public void setIdentificacao(String identificacao) {
        this.identificacao = identificacao;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public void setPrefixo(String prefixo) {
        this.prefixo = prefixo;
    }

    public TipoEquipamentoInventario getTipoEquipamento() {
        return tipoEquipamento;
    }

    public void setTipoEquipamento(TipoEquipamentoInventario tipoEquipamento) {
        this.tipoEquipamento = tipoEquipamento;
    }

    public TipoEquipamentoInventario.CategoriaEquipamento getCategoria() {
        return categoria;
    }

    public void setCategoria(TipoEquipamentoInventario.CategoriaEquipamento categoria) {
        this.categoria = categoria;
    }

    public EstadoUnidade getEstado() {
        return estado;
    }

    public void setEstado(EstadoUnidade estado) {
        this.estado = estado;
    }

    public CondicaoEquipamento getCondicao() {
        return condicao;
    }

    public void setCondicao(CondicaoEquipamento condicao) {
        this.condicao = condicao;
    }

    public StatusManutencao getStatusManutencao() {
        return statusManutencao;
    }

    public void setStatusManutencao(StatusManutencao statusManutencao) {
        this.statusManutencao = statusManutencao;
    }

    public String getProprietario() {
        return proprietario;
    }

    public void setProprietario(String proprietario) {
        this.proprietario = proprietario;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public String getPosicaoAtual() {
        return posicaoAtual;
    }

    public void setPosicaoAtual(String posicaoAtual) {
        this.posicaoAtual = posicaoAtual;
    }

    public String getPosicaoPlanejada() {
        return posicaoPlanejada;
    }

    public void setPosicaoPlanejada(String posicaoPlanejada) {
        this.posicaoPlanejada = posicaoPlanejada;
    }

    public BigDecimal getPesoBrutoKg() {
        return pesoBrutoKg;
    }

    public void setPesoBrutoKg(BigDecimal pesoBrutoKg) {
        this.pesoBrutoKg = pesoBrutoKg;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<LacreRegistro> getLacres() {
        return lacres;
    }

    public List<DocumentoRegistro> getDocumentos() {
        return documentos;
    }

    public List<AvariaRegistro> getAvarias() {
        return avarias;
    }

    public List<RestricaoRegistro> getRestricoes() {
        return restricoes;
    }

    public List<ManutencaoRegistro> getManutencoes() {
        return manutencoes;
    }

    public List<HistoricoAtributoRegistro> getHistoricoAtributos() {
        return historicoAtributos;
    }

    public List<ReeferRegistro> getRegistrosReefer() {
        return registrosReefer;
    }

    public Long getVersao() {
        return versao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @Embeddable
    public static class LacreRegistro {
        @Column(name = "numero", nullable = false, length = 60)
        private String numero;
        @Column(name = "tipo", length = 40)
        private String tipo;
        @Column(name = "status", nullable = false, length = 30)
        private String status;
        @Column(name = "anexado_em", nullable = false)
        private LocalDateTime anexadoEm;
        @Column(name = "removido_em")
        private LocalDateTime removidoEm;
        @Column(name = "responsavel", length = 120)
        private String responsavel;

        public LacreRegistro() {
        }

        public LacreRegistro(String numero, String tipo, String status, LocalDateTime anexadoEm,
                             LocalDateTime removidoEm, String responsavel) {
            this.numero = numero;
            this.tipo = tipo;
            this.status = status;
            this.anexadoEm = anexadoEm;
            this.removidoEm = removidoEm;
            this.responsavel = responsavel;
        }

        public String getNumero() { return numero; }
        public String getTipo() { return tipo; }
        public String getStatus() { return status; }
        public LocalDateTime getAnexadoEm() { return anexadoEm; }
        public LocalDateTime getRemovidoEm() { return removidoEm; }
        public String getResponsavel() { return responsavel; }
    }

    @Embeddable
    public static class DocumentoRegistro {
        @Column(name = "tipo_documento", nullable = false, length = 60)
        private String tipo;
        @Column(name = "numero_documento", length = 100)
        private String numero;
        @Column(name = "uri_documento", length = 500)
        private String uri;
        @Column(name = "checksum", length = 128)
        private String checksum;
        @Enumerated(EnumType.STRING)
        @Column(name = "status_documento", nullable = false, length = 30)
        private StatusDocumento status;
        @Column(name = "valido_ate")
        private LocalDate validoAte;
        @Column(name = "registrado_em", nullable = false)
        private LocalDateTime registradoEm;

        public DocumentoRegistro() {
        }

        public DocumentoRegistro(String tipo, String numero, String uri, String checksum,
                                 StatusDocumento status, LocalDate validoAte, LocalDateTime registradoEm) {
            this.tipo = tipo;
            this.numero = numero;
            this.uri = uri;
            this.checksum = checksum;
            this.status = status;
            this.validoAte = validoAte;
            this.registradoEm = registradoEm;
        }

        public String getTipo() { return tipo; }
        public String getNumero() { return numero; }
        public String getUri() { return uri; }
        public String getChecksum() { return checksum; }
        public StatusDocumento getStatus() { return status; }
        public LocalDate getValidoAte() { return validoAte; }
        public LocalDateTime getRegistradoEm() { return registradoEm; }
    }

    @Embeddable
    public static class AvariaRegistro {
        @Column(name = "componente", nullable = false, length = 80)
        private String componente;
        @Column(name = "tipo_avaria", nullable = false, length = 80)
        private String tipo;
        @Column(name = "severidade", nullable = false, length = 30)
        private String severidade;
        @Enumerated(EnumType.STRING)
        @Column(name = "status_avaria", nullable = false, length = 30)
        private StatusAvaria status;
        @Column(name = "descricao", length = 500)
        private String descricao;
        @Column(name = "detectada_em", nullable = false)
        private LocalDateTime detectadaEm;
        @Column(name = "reparada_em")
        private LocalDateTime reparadaEm;
        @Column(name = "responsavel", length = 120)
        private String responsavel;

        public AvariaRegistro() {
        }

        public AvariaRegistro(String componente, String tipo, String severidade, StatusAvaria status,
                              String descricao, LocalDateTime detectadaEm, LocalDateTime reparadaEm,
                              String responsavel) {
            this.componente = componente;
            this.tipo = tipo;
            this.severidade = severidade;
            this.status = status;
            this.descricao = descricao;
            this.detectadaEm = detectadaEm;
            this.reparadaEm = reparadaEm;
            this.responsavel = responsavel;
        }

        public String getComponente() { return componente; }
        public String getTipo() { return tipo; }
        public String getSeveridade() { return severidade; }
        public StatusAvaria getStatus() { return status; }
        public String getDescricao() { return descricao; }
        public LocalDateTime getDetectadaEm() { return detectadaEm; }
        public LocalDateTime getReparadaEm() { return reparadaEm; }
        public String getResponsavel() { return responsavel; }
    }

    @Embeddable
    public static class RestricaoRegistro {
        @Enumerated(EnumType.STRING)
        @Column(name = "tipo_restricao", nullable = false, length = 20)
        private TipoRestricao tipo;
        @Column(name = "codigo_restricao", nullable = false, length = 60)
        private String codigo;
        @Column(name = "descricao", length = 500)
        private String descricao;
        @Column(name = "autoridade", length = 120)
        private String autoridade;
        @Column(name = "ativa", nullable = false)
        private boolean ativa;
        @Column(name = "valido_de")
        private LocalDateTime validoDe;
        @Column(name = "valido_ate")
        private LocalDateTime validoAte;
        @Column(name = "registrado_em", nullable = false)
        private LocalDateTime registradoEm;

        public RestricaoRegistro() {
        }

        public RestricaoRegistro(TipoRestricao tipo, String codigo, String descricao, String autoridade,
                                 boolean ativa, LocalDateTime validoDe, LocalDateTime validoAte,
                                 LocalDateTime registradoEm) {
            this.tipo = tipo;
            this.codigo = codigo;
            this.descricao = descricao;
            this.autoridade = autoridade;
            this.ativa = ativa;
            this.validoDe = validoDe;
            this.validoAte = validoAte;
            this.registradoEm = registradoEm;
        }

        public TipoRestricao getTipo() { return tipo; }
        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
        public String getAutoridade() { return autoridade; }
        public boolean isAtiva() { return ativa; }
        public LocalDateTime getValidoDe() { return validoDe; }
        public LocalDateTime getValidoAte() { return validoAte; }
        public LocalDateTime getRegistradoEm() { return registradoEm; }
    }

    @Embeddable
    public static class ManutencaoRegistro {
        @Column(name = "ordem_servico", nullable = false, length = 60)
        private String ordemServico;
        @Column(name = "tipo_servico", nullable = false, length = 100)
        private String tipoServico;
        @Column(name = "fornecedor", length = 120)
        private String fornecedor;
        @Enumerated(EnumType.STRING)
        @Column(name = "status_manutencao_registro", nullable = false, length = 30)
        private StatusManutencao status;
        @Column(name = "aberta_em", nullable = false)
        private LocalDateTime abertaEm;
        @Column(name = "concluida_em")
        private LocalDateTime concluidaEm;
        @Column(name = "observacoes", length = 500)
        private String observacoes;

        public ManutencaoRegistro() {
        }

        public ManutencaoRegistro(String ordemServico, String tipoServico, String fornecedor,
                                  StatusManutencao status, LocalDateTime abertaEm,
                                  LocalDateTime concluidaEm, String observacoes) {
            this.ordemServico = ordemServico;
            this.tipoServico = tipoServico;
            this.fornecedor = fornecedor;
            this.status = status;
            this.abertaEm = abertaEm;
            this.concluidaEm = concluidaEm;
            this.observacoes = observacoes;
        }

        public String getOrdemServico() { return ordemServico; }
        public String getTipoServico() { return tipoServico; }
        public String getFornecedor() { return fornecedor; }
        public StatusManutencao getStatus() { return status; }
        public LocalDateTime getAbertaEm() { return abertaEm; }
        public LocalDateTime getConcluidaEm() { return concluidaEm; }
        public String getObservacoes() { return observacoes; }
    }

    @Embeddable
    public static class HistoricoAtributoRegistro {
        @Column(name = "atributo", nullable = false, length = 80)
        private String atributo;
        @Column(name = "valor_anterior", length = 1000)
        private String valorAnterior;
        @Column(name = "valor_atual", length = 1000)
        private String valorAtual;
        @Column(name = "origem", length = 80)
        private String origem;
        @Column(name = "responsavel", length = 120)
        private String responsavel;
        @Column(name = "alterado_em", nullable = false)
        private LocalDateTime alteradoEm;

        public HistoricoAtributoRegistro() {
        }

        public HistoricoAtributoRegistro(String atributo, String valorAnterior, String valorAtual,
                                         String origem, String responsavel, LocalDateTime alteradoEm) {
            this.atributo = atributo;
            this.valorAnterior = valorAnterior;
            this.valorAtual = valorAtual;
            this.origem = origem;
            this.responsavel = responsavel;
            this.alteradoEm = alteradoEm;
        }

        public String getAtributo() { return atributo; }
        public String getValorAnterior() { return valorAnterior; }
        public String getValorAtual() { return valorAtual; }
        public String getOrigem() { return origem; }
        public String getResponsavel() { return responsavel; }
        public LocalDateTime getAlteradoEm() { return alteradoEm; }
    }

    @Embeddable
    public static class ReeferRegistro {
        @Column(name = "setpoint_c", precision = 7, scale = 3)
        private BigDecimal setpointC;
        @Column(name = "temperatura_supply_c", precision = 7, scale = 3)
        private BigDecimal temperaturaSupplyC;
        @Column(name = "temperatura_return_c", precision = 7, scale = 3)
        private BigDecimal temperaturaReturnC;
        @Column(name = "umidade_percentual", precision = 6, scale = 3)
        private BigDecimal umidadePercentual;
        @Column(name = "ventilacao_m3h", precision = 9, scale = 3)
        private BigDecimal ventilacaoM3h;
        @Column(name = "ligado", nullable = false)
        private boolean ligado;
        @Column(name = "alarme", length = 255)
        private String alarme;
        @Column(name = "lido_em", nullable = false)
        private LocalDateTime lidoEm;
        @Column(name = "responsavel", length = 120)
        private String responsavel;

        public ReeferRegistro() {
        }

        public ReeferRegistro(BigDecimal setpointC, BigDecimal temperaturaSupplyC,
                              BigDecimal temperaturaReturnC, BigDecimal umidadePercentual,
                              BigDecimal ventilacaoM3h, boolean ligado, String alarme,
                              LocalDateTime lidoEm, String responsavel) {
            this.setpointC = setpointC;
            this.temperaturaSupplyC = temperaturaSupplyC;
            this.temperaturaReturnC = temperaturaReturnC;
            this.umidadePercentual = umidadePercentual;
            this.ventilacaoM3h = ventilacaoM3h;
            this.ligado = ligado;
            this.alarme = alarme;
            this.lidoEm = lidoEm;
            this.responsavel = responsavel;
        }

        public BigDecimal getSetpointC() { return setpointC; }
        public BigDecimal getTemperaturaSupplyC() { return temperaturaSupplyC; }
        public BigDecimal getTemperaturaReturnC() { return temperaturaReturnC; }
        public BigDecimal getUmidadePercentual() { return umidadePercentual; }
        public BigDecimal getVentilacaoM3h() { return ventilacaoM3h; }
        public boolean isLigado() { return ligado; }
        public String getAlarme() { return alarme; }
        public LocalDateTime getLidoEm() { return lidoEm; }
        public String getResponsavel() { return responsavel; }
    }
}
