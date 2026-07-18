package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.modelo.ReplanejamentoConteinerFerroviario;
import java.time.LocalDateTime;
import org.springframework.web.util.HtmlUtils;

public class ReplanejamentoConteinerHistoricoDto {

    private final Long id;
    private final String codigoConteiner;
    private final TipoMovimentacaoOrdem tipoMovimentacao;
    private final String vagaoOrigem;
    private final Integer posicaoOrigem;
    private final String vagaoDestino;
    private final Integer posicaoDestino;
    private final Integer ordemManifestoOrigem;
    private final Integer ordemManifestoDestino;
    private final String usuarioOperacao;
    private final String motivo;
    private final Long versaoAnterior;
    private final Long versaoAtual;
    private final LocalDateTime criadoEm;

    public ReplanejamentoConteinerHistoricoDto(Long id,
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
                                               Long versaoAtual,
                                               LocalDateTime criadoEm) {
        this.id = id;
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
        this.criadoEm = criadoEm;
    }

    public static ReplanejamentoConteinerHistoricoDto deEntidade(ReplanejamentoConteinerFerroviario entidade) {
        return new ReplanejamentoConteinerHistoricoDto(
                entidade.getId(),
                HtmlUtils.htmlEscape(entidade.getCodigoConteiner()),
                entidade.getTipoMovimentacao(),
                HtmlUtils.htmlEscape(entidade.getVagaoOrigem()),
                entidade.getPosicaoOrigem(),
                HtmlUtils.htmlEscape(entidade.getVagaoDestino()),
                entidade.getPosicaoDestino(),
                entidade.getOrdemManifestoOrigem(),
                entidade.getOrdemManifestoDestino(),
                HtmlUtils.htmlEscape(entidade.getUsuarioOperacao()),
                HtmlUtils.htmlEscape(entidade.getMotivo()),
                entidade.getVersaoAnterior(),
                entidade.getVersaoAtual(),
                entidade.getCriadoEm()
        );
    }

    public Long getId() {
        return id;
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
