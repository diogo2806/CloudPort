package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "lote_carga")
public class LoteCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemConhecimentoCarga item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_pai_id")
    private LoteCarga lotePai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NaturezaCarga natureza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusLoteCarga status = StatusLoteCarga.PROGRAMADO;

    @Column(name = "quantidade_prevista", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadePrevista;

    @Column(name = "volume_previsto_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumePrevistoM3;

    @Column(name = "peso_previsto_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoPrevistoKg;

    @Column(name = "quantidade_saldo", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeSaldo = BigDecimal.ZERO;

    @Column(name = "volume_saldo_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeSaldoM3 = BigDecimal.ZERO;

    @Column(name = "peso_saldo_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoSaldoKg = BigDecimal.ZERO;

    @Column(name = "quantidade_bloqueada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeBloqueada = BigDecimal.ZERO;

    @Column(name = "volume_bloqueado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeBloqueadoM3 = BigDecimal.ZERO;

    @Column(name = "peso_bloqueado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoBloqueadoKg = BigDecimal.ZERO;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Column(name = "marcas_embalagem", length = 300)
    private String marcasEmbalagem;

    @Column(name = "armazem_id", length = 80)
    private String armazemId;

    @Column(name = "posicao_armazenagem", length = 120)
    private String posicaoArmazenagem;

    @Column(name = "veiculo_id", length = 80)
    private String veiculoId;

    @Column(name = "visita_navio_id", length = 80)
    private String visitaNavioId;

    @Column(name = "cliente_id", length = 80)
    private String clienteId;

    @Column(name = "codigo_avaria", length = 80)
    private String codigoAvaria;

    @Column(name = "descricao_avaria", length = 1000)
    private String descricaoAvaria;

    @Column(name = "visita_trem_id", length = 80)
    private String visitaTremId;

    @Column(name = "vagao_id", length = 120)
    private String vagaoId;

    @Column(name = "posicao_ferroviaria", length = 120)
    private String posicaoFerroviaria;

    @Column(name = "sequencia_ferroviaria")
    private Integer sequenciaFerroviaria;

    @Column(name = "capacidade_vagao_peso_kg", precision = 19, scale = 3)
    private BigDecimal capacidadeVagaoPesoKg;

    @Column(name = "incompatibilidades_ferroviarias", length = 1000)
    private String incompatibilidadesFerroviarias;

    @Column(name = "custodia_ferroviaria", length = 120)
    private String custodiaFerroviaria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_ordem_ferroviaria", length = 20)
    private StatusOrdemFerroviariaCarga statusOrdemFerroviaria;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm DESC")
    private List<MovimentacaoCarga> movimentacoes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "historico_custodia_ferroviaria_carga",
            joinColumns = @JoinColumn(name = "lote_id", nullable = false))
    @OrderBy("ocorridoEm DESC")
    private List<HistoricoCustodiaFerroviaria> historicoCustodiaFerroviaria = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        codigo = normalizar(codigo);
        unidadeMedida = normalizar(unidadeMedida);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        codigo = normalizar(codigo);
        unidadeMedida = normalizar(unidadeMedida);
    }

    public void adicionarSaldo(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        quantidadeSaldo = quantidadeSaldo.add(valor(quantidade));
        volumeSaldoM3 = volumeSaldoM3.add(valor(volume));
        pesoSaldoKg = pesoSaldoKg.add(valor(peso));
        status = StatusLoteCarga.NO_TERMINAL;
    }

    public void retirarSaldo(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        BigDecimal quantidadeRetirada = valor(quantidade);
        BigDecimal volumeRetirado = valor(volume);
        BigDecimal pesoRetirado = valor(peso);
        if (quantidadeRetirada.compareTo(getQuantidadeDisponivel()) > 0
                || volumeRetirado.compareTo(getVolumeDisponivelM3()) > 0
                || pesoRetirado.compareTo(getPesoDisponivelKg()) > 0) {
            throw new IllegalStateException("Movimentação excede o saldo disponível e não bloqueado do lote.");
        }
        quantidadeSaldo = quantidadeSaldo.subtract(quantidadeRetirada);
        volumeSaldoM3 = volumeSaldoM3.subtract(volumeRetirado);
        pesoSaldoKg = pesoSaldoKg.subtract(pesoRetirado);
        status = quantidadeSaldo.signum() == 0 && volumeSaldoM3.signum() == 0 && pesoSaldoKg.signum() == 0
                ? StatusLoteCarga.CONCLUIDO
                : StatusLoteCarga.PARCIALMENTE_CARREGADO;
    }

    public void bloquearSaldo(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        BigDecimal quantidadeNova = valor(quantidade);
        BigDecimal volumeNovo = valor(volume);
        BigDecimal pesoNovo = valor(peso);
        if (quantidadeNova.compareTo(getQuantidadeDisponivel()) > 0
                || volumeNovo.compareTo(getVolumeDisponivelM3()) > 0
                || pesoNovo.compareTo(getPesoDisponivelKg()) > 0) {
            throw new IllegalStateException("Avaria excede o saldo disponível do lote.");
        }
        quantidadeBloqueada = quantidadeBloqueada.add(quantidadeNova);
        volumeBloqueadoM3 = volumeBloqueadoM3.add(volumeNovo);
        pesoBloqueadoKg = pesoBloqueadoKg.add(pesoNovo);
        status = StatusLoteCarga.AVARIADO;
    }

    public void liberarSaldoBloqueado(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        validarSaldoBloqueado(quantidade, volume, peso);
        quantidadeBloqueada = quantidadeBloqueada.subtract(valor(quantidade));
        volumeBloqueadoM3 = volumeBloqueadoM3.subtract(valor(volume));
        pesoBloqueadoKg = pesoBloqueadoKg.subtract(valor(peso));
        if (quantidadeBloqueada.signum() == 0 && volumeBloqueadoM3.signum() == 0 && pesoBloqueadoKg.signum() == 0) {
            status = quantidadeSaldo.signum() == 0 ? StatusLoteCarga.CONCLUIDO : StatusLoteCarga.NO_TERMINAL;
        }
    }

    public void baixarSaldoBloqueado(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        validarSaldoBloqueado(quantidade, volume, peso);
        quantidadeBloqueada = quantidadeBloqueada.subtract(valor(quantidade));
        volumeBloqueadoM3 = volumeBloqueadoM3.subtract(valor(volume));
        pesoBloqueadoKg = pesoBloqueadoKg.subtract(valor(peso));
        quantidadeSaldo = quantidadeSaldo.subtract(valor(quantidade));
        volumeSaldoM3 = volumeSaldoM3.subtract(valor(volume));
        pesoSaldoKg = pesoSaldoKg.subtract(valor(peso));
        status = quantidadeSaldo.signum() == 0 && volumeSaldoM3.signum() == 0 && pesoSaldoKg.signum() == 0
                ? StatusLoteCarga.CONCLUIDO
                : quantidadeBloqueada.signum() > 0 || volumeBloqueadoM3.signum() > 0 || pesoBloqueadoKg.signum() > 0
                    ? StatusLoteCarga.AVARIADO
                    : StatusLoteCarga.NO_TERMINAL;
    }

    public void planejarFerrovia(
            String visitaTrem,
            String vagao,
            String posicao,
            int sequencia,
            BigDecimal capacidadePesoKg,
            String incompatibilidades,
            String custodia,
            String responsavel) {
        validarTexto(visitaTrem, "A visita de trem deve ser informada.");
        validarTexto(vagao, "O vagão deve ser informado.");
        validarTexto(custodia, "A custódia deve ser informada.");
        validarTexto(responsavel, "O responsável deve ser informado.");
        if (sequencia <= 0) {
            throw new IllegalArgumentException("A sequência ferroviária deve ser maior que zero.");
        }
        if (capacidadePesoKg == null || capacidadePesoKg.signum() <= 0) {
            throw new IllegalArgumentException("A capacidade de peso do vagão deve ser maior que zero.");
        }
        BigDecimal pesoOperacional = pesoSaldoKg.signum() > 0 ? pesoSaldoKg : pesoPrevistoKg;
        if (pesoOperacional != null && pesoOperacional.compareTo(capacidadePesoKg) > 0) {
            throw new IllegalStateException("O peso do cargo lot excede a capacidade do vagão.");
        }

        StatusOrdemFerroviariaCarga statusAnterior = statusOrdemFerroviaria;
        String custodiaAnterior = custodiaFerroviaria;
        visitaTremId = visitaTrem.trim();
        vagaoId = vagao.trim();
        posicaoFerroviaria = textoOuNulo(posicao);
        sequenciaFerroviaria = sequencia;
        capacidadeVagaoPesoKg = capacidadePesoKg;
        incompatibilidadesFerroviarias = textoOuNulo(incompatibilidades);
        custodiaFerroviaria = custodia.trim();
        statusOrdemFerroviaria = StatusOrdemFerroviariaCarga.PENDENTE;
        registrarHistoricoCustodia(
                statusAnterior,
                statusOrdemFerroviaria,
                custodiaAnterior,
                custodiaFerroviaria,
                "PLANEJAMENTO",
                "Ordem ferroviária planejada ou replanejada.",
                responsavel);
    }

    public void atualizarStatusFerroviario(
            StatusOrdemFerroviariaCarga novoStatus,
            String novaCustodia,
            String motivo,
            String responsavel) {
        if (statusOrdemFerroviaria == null || visitaTremId == null) {
            throw new IllegalStateException("O cargo lot não possui ordem ferroviária planejada.");
        }
        if (novoStatus == null) {
            throw new IllegalArgumentException("O status da ordem ferroviária deve ser informado.");
        }
        validarTexto(responsavel, "O responsável deve ser informado.");
        validarTransicaoStatusFerroviario(statusOrdemFerroviaria, novoStatus);

        String custodiaNova = textoOuNulo(novaCustodia);
        if (custodiaNova == null) {
            custodiaNova = custodiaFerroviaria;
        }
        if (Objects.equals(statusOrdemFerroviaria, novoStatus)
                && Objects.equals(custodiaFerroviaria, custodiaNova)) {
            return;
        }

        StatusOrdemFerroviariaCarga statusAnterior = statusOrdemFerroviaria;
        String custodiaAnterior = custodiaFerroviaria;
        statusOrdemFerroviaria = novoStatus;
        custodiaFerroviaria = custodiaNova;
        registrarHistoricoCustodia(
                statusAnterior,
                novoStatus,
                custodiaAnterior,
                custodiaNova,
                "ATUALIZACAO_STATUS",
                motivo,
                responsavel);
    }

    private void validarTransicaoStatusFerroviario(
            StatusOrdemFerroviariaCarga atual,
            StatusOrdemFerroviariaCarga novoStatus) {
        if (atual == novoStatus) {
            return;
        }
        if (atual == StatusOrdemFerroviariaCarga.PENDENTE
                && (novoStatus == StatusOrdemFerroviariaCarga.EM_EXECUCAO
                || novoStatus == StatusOrdemFerroviariaCarga.CONCLUIDA)) {
            return;
        }
        if (atual == StatusOrdemFerroviariaCarga.EM_EXECUCAO
                && novoStatus == StatusOrdemFerroviariaCarga.CONCLUIDA) {
            return;
        }
        throw new IllegalStateException(
                "A transição ferroviária de " + atual + " para " + novoStatus + " não é permitida.");
    }

    private void registrarHistoricoCustodia(
            StatusOrdemFerroviariaCarga statusAnterior,
            StatusOrdemFerroviariaCarga statusNovo,
            String custodiaAnterior,
            String custodiaNova,
            String evento,
            String motivo,
            String responsavel) {
        historicoCustodiaFerroviaria.add(new HistoricoCustodiaFerroviaria(
                statusAnterior,
                statusNovo,
                custodiaAnterior,
                custodiaNova,
                evento,
                textoOuNulo(motivo),
                responsavel.trim(),
                OffsetDateTime.now()));
    }

    private void validarSaldoBloqueado(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (valor(quantidade).compareTo(quantidadeBloqueada) > 0
                || valor(volume).compareTo(volumeBloqueadoM3) > 0
                || valor(peso).compareTo(pesoBloqueadoKg) > 0) {
            throw new IllegalStateException("Operação excede o saldo bloqueado pela avaria.");
        }
    }

    public void registrarMovimentacao(MovimentacaoCarga movimentacao) {
        movimentacao.setLote(this);
        movimentacoes.add(movimentacao);
    }

    public void atualizarLocalizacao(String armazem, String posicao, String veiculo, String visitaNavio, String cliente) {
        if (armazem != null) armazemId = armazem;
        if (posicao != null) posicaoArmazenagem = posicao;
        if (veiculo != null) veiculoId = veiculo;
        if (visitaNavio != null) visitaNavioId = visitaNavio;
        if (cliente != null) clienteId = cliente;
    }

    private BigDecimal valor(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
    private String normalizar(String valor) { return valor == null ? null : valor.trim().toUpperCase(); }
    private String textoOuNulo(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    private void validarTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public ItemConhecimentoCarga getItem() { return item; }
    public void setItem(ItemConhecimentoCarga item) { this.item = item; }
    public LoteCarga getLotePai() { return lotePai; }
    public void setLotePai(LoteCarga lotePai) { this.lotePai = lotePai; }
    public NaturezaCarga getNatureza() { return natureza; }
    public void setNatureza(NaturezaCarga natureza) { this.natureza = natureza; }
    public StatusLoteCarga getStatus() { return status; }
    public void setStatus(StatusLoteCarga status) { this.status = status; }
    public BigDecimal getQuantidadePrevista() { return quantidadePrevista; }
    public void setQuantidadePrevista(BigDecimal quantidadePrevista) { this.quantidadePrevista = quantidadePrevista; }
    public BigDecimal getVolumePrevistoM3() { return volumePrevistoM3; }
    public void setVolumePrevistoM3(BigDecimal volumePrevistoM3) { this.volumePrevistoM3 = volumePrevistoM3; }
    public BigDecimal getPesoPrevistoKg() { return pesoPrevistoKg; }
    public void setPesoPrevistoKg(BigDecimal pesoPrevistoKg) { this.pesoPrevistoKg = pesoPrevistoKg; }
    public BigDecimal getQuantidadeSaldo() { return quantidadeSaldo; }
    public BigDecimal getVolumeSaldoM3() { return volumeSaldoM3; }
    public BigDecimal getPesoSaldoKg() { return pesoSaldoKg; }
    public BigDecimal getQuantidadeBloqueada() { return quantidadeBloqueada; }
    public BigDecimal getVolumeBloqueadoM3() { return volumeBloqueadoM3; }
    public BigDecimal getPesoBloqueadoKg() { return pesoBloqueadoKg; }
    public BigDecimal getQuantidadeDisponivel() { return quantidadeSaldo.subtract(quantidadeBloqueada); }
    public BigDecimal getVolumeDisponivelM3() { return volumeSaldoM3.subtract(volumeBloqueadoM3); }
    public BigDecimal getPesoDisponivelKg() { return pesoSaldoKg.subtract(pesoBloqueadoKg); }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public String getMarcasEmbalagem() { return marcasEmbalagem; }
    public void setMarcasEmbalagem(String marcasEmbalagem) { this.marcasEmbalagem = marcasEmbalagem; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getPosicaoArmazenagem() { return posicaoArmazenagem; }
    public void setPosicaoArmazenagem(String posicaoArmazenagem) { this.posicaoArmazenagem = posicaoArmazenagem; }
    public String getVeiculoId() { return veiculoId; }
    public void setVeiculoId(String veiculoId) { this.veiculoId = veiculoId; }
    public String getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(String visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getCodigoAvaria() { return codigoAvaria; }
    public void setCodigoAvaria(String codigoAvaria) { this.codigoAvaria = codigoAvaria; }
    public String getDescricaoAvaria() { return descricaoAvaria; }
    public void setDescricaoAvaria(String descricaoAvaria) { this.descricaoAvaria = descricaoAvaria; }
    public String getVisitaTremId() { return visitaTremId; }
    public String getVagaoId() { return vagaoId; }
    public String getPosicaoFerroviaria() { return posicaoFerroviaria; }
    public Integer getSequenciaFerroviaria() { return sequenciaFerroviaria; }
    public BigDecimal getCapacidadeVagaoPesoKg() { return capacidadeVagaoPesoKg; }
    public String getIncompatibilidadesFerroviarias() { return incompatibilidadesFerroviarias; }
    public String getCustodiaFerroviaria() { return custodiaFerroviaria; }
    public StatusOrdemFerroviariaCarga getStatusOrdemFerroviaria() { return statusOrdemFerroviaria; }
    public List<HistoricoCustodiaFerroviaria> getHistoricoCustodiaFerroviaria() {
        return Collections.unmodifiableList(historicoCustodiaFerroviaria);
    }
    public List<MovimentacaoCarga> getMovimentacoes() { return Collections.unmodifiableList(movimentacoes); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }

    @Embeddable
    public static class HistoricoCustodiaFerroviaria {

        @Enumerated(EnumType.STRING)
        @Column(name = "status_anterior", length = 20)
        private StatusOrdemFerroviariaCarga statusAnterior;

        @Enumerated(EnumType.STRING)
        @Column(name = "status_novo", nullable = false, length = 20)
        private StatusOrdemFerroviariaCarga statusNovo;

        @Column(name = "custodia_anterior", length = 120)
        private String custodiaAnterior;

        @Column(name = "custodia_nova", length = 120)
        private String custodiaNova;

        @Column(nullable = false, length = 40)
        private String evento;

        @Column(length = 1000)
        private String motivo;

        @Column(nullable = false, length = 120)
        private String responsavel;

        @Column(name = "ocorrido_em", nullable = false)
        private OffsetDateTime ocorridoEm;

        protected HistoricoCustodiaFerroviaria() {
        }

        private HistoricoCustodiaFerroviaria(
                StatusOrdemFerroviariaCarga statusAnterior,
                StatusOrdemFerroviariaCarga statusNovo,
                String custodiaAnterior,
                String custodiaNova,
                String evento,
                String motivo,
                String responsavel,
                OffsetDateTime ocorridoEm) {
            this.statusAnterior = statusAnterior;
            this.statusNovo = statusNovo;
            this.custodiaAnterior = custodiaAnterior;
            this.custodiaNova = custodiaNova;
            this.evento = evento;
            this.motivo = motivo;
            this.responsavel = responsavel;
            this.ocorridoEm = ocorridoEm;
        }

        public StatusOrdemFerroviariaCarga getStatusAnterior() { return statusAnterior; }
        public StatusOrdemFerroviariaCarga getStatusNovo() { return statusNovo; }
        public String getCustodiaAnterior() { return custodiaAnterior; }
        public String getCustodiaNova() { return custodiaNova; }
        public String getEvento() { return evento; }
        public String getMotivo() { return motivo; }
        public String getResponsavel() { return responsavel; }
        public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
    }
}
