package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class MovimentarConteinerPatioDto {

    @NotNull
    @Min(0)
    private Integer linhaDestino;

    @NotNull
    @Min(0)
    private Integer colunaDestino;

    @NotBlank
    @Size(max = 40)
    private String camadaDestino;

    @NotBlank
    @Size(max = 255)
    private String motivo;

    @Size(max = 120)
    private String usuario;

    @Size(max = 80)
    private String origemAcao;

    @Size(max = 120)
    private String correlationId;

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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = ValidacaoEntradaUtil.limparTexto(motivo);
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = ValidacaoEntradaUtil.limparTexto(usuario);
    }

    public String getOrigemAcao() {
        return origemAcao;
    }

    public void setOrigemAcao(String origemAcao) {
        this.origemAcao = ValidacaoEntradaUtil.limparTexto(origemAcao);
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = ValidacaoEntradaUtil.limparTexto(correlationId);
    }
}