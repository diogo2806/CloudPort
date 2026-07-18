package br.com.cloudport.servicorail.ferrovia.modelo;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "replanejamento_conteiner_ferroviario")
public class ReplanejamentoConteinerFerroviario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visita_trem_id", nullable = false)
    private Long visitaTremId;

    @Column(name = "codigo_conteiner", nullable = false, length = 20)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false, length = 20)
    private TipoMovimentacaoOrdem tipoMovimentacao;

    @Column(name = "vagao_origem", nullable = false, length = 35)
    private String vagaoOrigem;

    @Column(name = "posicao_origem", nullable = false)
    private Integer posicaoOrigem;

    @Column(name = "vagao_destino", nullable = false, length = 35)
    private String vagaoDestino;

    @Column(name = "posicao_destino", nullable = false)
    private Integer posicaoDestino;

    @Column(name = "ordem_manifesto_origem", nullable = false)
    private Integer ordemManifestoOrigem;

    @Column(name = "ordem_manifesto_destino", nullable = false)
    private Integer ordemManifestoDestino;

    @Column(name = "usuario_operacao", nullable = false, length = 120)
    private String usuarioOperacao;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "versao_anterior", nullable = false)
    private Long versaoAnterior;

    @Column(name = "versao_atual", nullable = false)
    private Long versaoAtual;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public ReplanejamentoConteinerFerroviario() {
    }

    public ReplanejamentoConteinerFerroviario(Long visitaTremId,
                                              String codigoConteiner,
                                              TipoMovimentacaoOrdem tipoMovimentacao,
                                              String vagaoOrigem,
                                              Integer posicaoOrigem,
                                              String vagaoDestino,
                                              Integer posicaoDestino,
                                              Integer ordemManifestoOrigem,
                                              Integer ordemManifestoDestino,
                                              String usuarioOperacao,
                                              String motivo,
                                              Long versaoAnterior,
                                              Long versaoAtual) {
        this.visitaTremId = visitaTremId;
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimentacao = tipoMovimentacao;
        this.vagaoOrigem = vagaoOrigem;
        this.posicaoOrigem = posicaoOrigem;
        this.vagaoDestino = vagaoDestino;
        this.posicaoDestino = posicaoDestino;
        this.ordemManifestoOrigem = ordemManifestoOrigem;
        this.ordemManifestoDestino = ordemManifestoDestino;
        this.usuarioOperacao = usuarioOperacao;
        this.motivo = motivo;
        this.versaoAnterior = versaoAnterior;
        this.versaoAtual = versaoAtual;
    }

    @PrePersist
    public void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getVisitaTremId() {
        return visitaTremId;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public TipoMovimentacaoOrdem getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public String getVagaoOrigem() {
        return vagaoOrigem;
    }

    public Integer getPosicaoOrigem() {
        return posicaoOrigem;
    }

    public String getVagaoDestino() {
        return vagaoDestino;
    }

    public Integer getPosicaoDestino() {
        return posicaoDestino;
    }

    public Integer getOrdemManifestoOrigem() {
        return ordemManifestoOrigem;
    }

    public Integer getOrdemManifestoDestino() {
        return ordemManifestoDestino;
    }

    public String getUsuarioOperacao() {
        return usuarioOperacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public Long getVersaoAnterior() {
        return versaoAnterior;
    }

    public Long getVersaoAtual() {
        return versaoAtual;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
