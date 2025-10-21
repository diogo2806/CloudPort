package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.util.StringUtils;

public class OrdemTrabalhoPatioRequisicaoDto {

    @NotBlank
    @Size(max = 30)
    private String codigoConteiner;

    @Size(max = 40)
    private String tipoCarga;

    @NotBlank
    @Size(max = 60)
    private String destino;

    @NotNull
    @Min(0)
    private Integer linhaDestino;

    @NotNull
    @Min(0)
    private Integer colunaDestino;

    @NotBlank
    @Size(max = 40)
    private String camadaDestino;

    @NotNull
    private TipoMovimentoPatio tipoMovimento;

    @NotNull
    private StatusConteiner statusConteinerDestino;

    public OrdemTrabalhoPatioRequisicaoDto() {
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = ValidacaoEntradaUtil.limparTexto(codigoConteiner);
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        String valorLimpo = ValidacaoEntradaUtil.limparTexto(tipoCarga);
        this.tipoCarga = StringUtils.hasText(valorLimpo) ? valorLimpo : null;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = ValidacaoEntradaUtil.limparTexto(destino);
    }

    public Integer getLinhaDestino() {
        return linhaDestino;
    }

    public void setLinhaDestino(Integer linhaDestino) {
        this.linhaDestino = linhaDestino;
    }

    public Integer getColunaDestino() {
        return colunaDestino;
    }

    public void setColunaDestino(Integer colunaDestino) {
        this.colunaDestino = colunaDestino;
    }

    public String getCamadaDestino() {
        return camadaDestino;
    }

    public void setCamadaDestino(String camadaDestino) {
        this.camadaDestino = ValidacaoEntradaUtil.limparTexto(camadaDestino);
    }

    public TipoMovimentoPatio getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public StatusConteiner getStatusConteinerDestino() {
        return statusConteinerDestino;
    }

    public void setStatusConteinerDestino(StatusConteiner statusConteinerDestino) {
        this.statusConteinerDestino = statusConteinerDestino;
    }
}
