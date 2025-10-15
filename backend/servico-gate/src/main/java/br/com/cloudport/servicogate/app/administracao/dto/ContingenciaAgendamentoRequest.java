package br.com.cloudport.servicogate.app.administracao.dto;

import javax.validation.constraints.NotBlank;

public class ContingenciaAgendamentoRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String motivo;

    @NotBlank
    private String operador;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }
}
