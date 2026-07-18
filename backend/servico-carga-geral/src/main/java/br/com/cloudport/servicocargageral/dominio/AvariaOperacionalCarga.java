package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.ResultadoAvaria;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusAvariaOperacional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
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

@Entity
@Table(name = "avaria_operacional_carga")
public class AvariaOperacionalCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCarga lote;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "quantidade_afetada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeAfetada;

    @Column(name = "volume_afetado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeAfetadoM3;

    @Column(name = "peso_afetado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoAfetadoKg;

    @Column(nullable = false, length = 120)
    private String responsavel;

    @Column(name = "evidencias_json", length = 8000)
    private String evidenciasJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAvariaOperacional status = StatusAvariaOperacional.ABERTA;

    @Column(name = "relatorio_inspecao", length = 4000)
    private String relatorioInspecao;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_tratamento", length = 30)
    private ResultadoAvaria resultadoTratamento;

    @Column(name = "observacao_encerramento", length = 2000)
    private String observacaoEncerramento;

    @Column(name = "historico_operacional", nullable = false, length = 16000)
    private String historicoOperacional = "";

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "encerrado_em")
    private OffsetDateTime encerradoEm;

    @Version
    private Long versao;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        normalizar();
        validarQuantidades();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        normalizar();
        validarQuantidades();
    }

    public void segregar(String usuario) {
        if (status != StatusAvariaOperacional.ABERTA) {
            throw new IllegalStateException("Somente avaria aberta pode ser segregada.");
        }
        status = StatusAvariaOperacional.SEGREGADA;
        registrarHistorico("SEGREGADA", usuario, "Saldo afetado segregado do estoque disponível.");
    }

    public void iniciarInspecao(String relatorio, String usuario) {
        if (status != StatusAvariaOperacional.SEGREGADA && status != StatusAvariaOperacional.ABERTA) {
            throw new IllegalStateException("Avaria não permite inspeção no estado atual.");
        }
        if (relatorio == null || relatorio.isBlank()) {
            throw new IllegalArgumentException("O relatório de inspeção é obrigatório.");
        }
        relatorioInspecao = relatorio.trim();
        status = StatusAvariaOperacional.EM_TRATAMENTO;
        registrarHistorico("INSPECAO", usuario, relatorioInspecao);
    }

    public void encerrar(ResultadoAvaria resultado, String observacao, String usuario) {
        if (status == StatusAvariaOperacional.REINTEGRADA
                || status == StatusAvariaOperacional.BAIXADA
                || status == StatusAvariaOperacional.BLOQUEADA
                || status == StatusAvariaOperacional.ENCERRADA) {
            throw new IllegalStateException("Avaria já está encerrada.");
        }
        resultadoTratamento = resultado;
        observacaoEncerramento = observacao == null ? null : observacao.trim();
        status = switch (resultado) {
            case REINTEGRAR -> StatusAvariaOperacional.REINTEGRADA;
            case BAIXAR -> StatusAvariaOperacional.BAIXADA;
            case MANTER_BLOQUEADA -> StatusAvariaOperacional.BLOQUEADA;
        };
        encerradoEm = OffsetDateTime.now();
        registrarHistorico("ENCERRADA", usuario, resultado.name() + ": " + observacaoEncerramento);
    }

    public void registrarHistorico(String evento, String usuario, String detalhe) {
        String linha = OffsetDateTime.now() + "|" + normalizarUsuario(usuario) + "|" + evento + "|"
                + (detalhe == null ? "" : detalhe.trim());
        historicoOperacional = historicoOperacional == null || historicoOperacional.isBlank()
                ? linha : historicoOperacional + "\n" + linha;
    }

    private void normalizar() {
        codigo = obrigatorio(codigo).toUpperCase();
        descricao = obrigatorio(descricao);
        responsavel = obrigatorio(responsavel);
    }

    private void validarQuantidades() {
        quantidadeAfetada = valor(quantidadeAfetada);
        volumeAfetadoM3 = valor(volumeAfetadoM3);
        pesoAfetadoKg = valor(pesoAfetadoKg);
        if (quantidadeAfetada.signum() <= 0 || volumeAfetadoM3.signum() < 0 || pesoAfetadoKg.signum() < 0) {
            throw new IllegalStateException("A avaria deve possuir quantidade positiva e volume e peso não negativos.");
        }
    }

    private BigDecimal valor(BigDecimal numero) {
        return numero == null ? BigDecimal.ZERO : numero;
    }

    private String obrigatorio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Campo obrigatório da avaria não informado.");
        }
        return valor.trim();
    }

    private String normalizarUsuario(String usuario) {
        return usuario == null || usuario.isBlank() ? "SISTEMA" : usuario.trim();
    }

    public UUID getId() { return id; }
    public LoteCarga getLote() { return lote; }
    public void setLote(LoteCarga lote) { this.lote = lote; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getQuantidadeAfetada() { return quantidadeAfetada; }
    public void setQuantidadeAfetada(BigDecimal quantidadeAfetada) { this.quantidadeAfetada = quantidadeAfetada; }
    public BigDecimal getVolumeAfetadoM3() { return volumeAfetadoM3; }
    public void setVolumeAfetadoM3(BigDecimal volumeAfetadoM3) { this.volumeAfetadoM3 = volumeAfetadoM3; }
    public BigDecimal getPesoAfetadoKg() { return pesoAfetadoKg; }
    public void setPesoAfetadoKg(BigDecimal pesoAfetadoKg) { this.pesoAfetadoKg = pesoAfetadoKg; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getEvidenciasJson() { return evidenciasJson; }
    public void setEvidenciasJson(String evidenciasJson) { this.evidenciasJson = evidenciasJson; }
    public StatusAvariaOperacional getStatus() { return status; }
    public String getRelatorioInspecao() { return relatorioInspecao; }
    public ResultadoAvaria getResultadoTratamento() { return resultadoTratamento; }
    public String getObservacaoEncerramento() { return observacaoEncerramento; }
    public String getHistoricoOperacional() { return historicoOperacional; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getEncerradoEm() { return encerradoEm; }
}
