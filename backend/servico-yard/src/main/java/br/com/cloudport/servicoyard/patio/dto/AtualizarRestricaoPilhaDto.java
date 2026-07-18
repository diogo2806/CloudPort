package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AtualizarRestricaoPilhaDto {

    private Boolean bloqueada;
    private Boolean interditada;
    private Boolean areaPermitida;

    @Size(max = 500)
    private String notaOperacional;

    @NotBlank
    @Size(max = 255)
    private String motivo;

    @Size(max = 120)
    private String usuario;

    @Size(max = 80)
    private String origemAcao;

    @Size(max = 120)
    private String correlationId;

    public Boolean getBloqueada() {
        return bloqueada;
    }

    public void setBloqueada(Boolean bloqueada) {
        this.bloqueada = bloqueada;
    }

    public Boolean getInterditada() {
        return interditada;
    }

    public void setInterditada(Boolean interditada) {
        this.interditada = interditada;
    }

    public Boolean getAreaPermitida() {
        return areaPermitida;
    }

    public void setAreaPermitida(Boolean areaPermitida) {
        this.areaPermitida = areaPermitida;
    }

    public String getNotaOperacional() {
        return notaOperacional;
    }

    public void setNotaOperacional(String notaOperacional) {
        this.notaOperacional = ValidacaoEntradaUtil.limparTexto(notaOperacional);
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