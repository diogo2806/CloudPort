package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import java.time.LocalDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ConteinerPatioRequisicaoDto {

    private Long id;

    @NotBlank
    @Size(max = 30)
    private String codigo;

    @NotNull
    @Min(0)
    private Integer linha;

    @NotNull
    @Min(0)
    private Integer coluna;

    @NotNull
    private StatusConteiner status;

    @NotBlank
    @Size(max = 40)
    private String tipoCarga;

    @NotBlank
    @Size(max = 60)
    private String destino;

    @NotBlank
    @Size(max = 40)
    private String camadaOperacional;

    public ConteinerPatioRequisicaoDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = ValidacaoEntradaUtil.limparTexto(codigo);
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public StatusConteiner getStatus() {
        return status;
    }

    public void setStatus(StatusConteiner status) {
        this.status = status;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = ValidacaoEntradaUtil.limparTexto(tipoCarga);
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = ValidacaoEntradaUtil.limparTexto(destino);
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public void setCamadaOperacional(String camadaOperacional) {
        this.camadaOperacional = ValidacaoEntradaUtil.limparTexto(camadaOperacional);
    }

    public LocalDateTime gerarHorarioAtualizacao() {
        return LocalDateTime.now();
    }
}
