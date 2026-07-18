package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ReplanejamentoConteinerRequisicaoDto {

    @NotBlank
    @Size(max = 20)
    private String codigoConteiner;

    @NotNull
    private TipoMovimentacaoOrdem tipoMovimentacao;

    @NotBlank
    @Size(max = 35)
    private String vagaoOrigem;

    @NotBlank
    @Size(max = 35)
    private String vagaoDestino;

    @NotNull
    @Min(0)
    private Long versaoComposicao;

    @Min(1)
    private Integer ordemManifestoDestino;

    @NotBlank
    @Size(max = 500)
    private String motivo;

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = ValidacaoEntradaUtil.limparTexto(codigoConteiner);
    }

    public TipoMovimentacaoOrdem getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(TipoMovimentacaoOrdem tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public String getVagaoOrigem() {
        return vagaoOrigem;
    }

    public void setVagaoOrigem(String vagaoOrigem) {
        this.vagaoOrigem = ValidacaoEntradaUtil.limparTexto(vagaoOrigem);
    }

    public String getVagaoDestino() {
        return vagaoDestino;
    }

    public void setVagaoDestino(String vagaoDestino) {
        this.vagaoDestino = ValidacaoEntradaUtil.limparTexto(vagaoDestino);
    }

    public Long getVersaoComposicao() {
        return versaoComposicao;
    }

    public void setVersaoComposicao(Long versaoComposicao) {
        this.versaoComposicao = versaoComposicao;
    }

    public Integer getOrdemManifestoDestino() {
        return ordemManifestoDestino;
    }

    public void setOrdemManifestoDestino(Integer ordemManifestoDestino) {
        this.ordemManifestoDestino = ordemManifestoDestino;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = ValidacaoEntradaUtil.limparTexto(motivo);
    }
}
