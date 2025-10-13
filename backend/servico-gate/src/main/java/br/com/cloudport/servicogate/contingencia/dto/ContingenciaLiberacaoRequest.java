package br.com.cloudport.servicogate.contingencia.dto;

import javax.validation.constraints.NotBlank;

public class ContingenciaLiberacaoRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String operador;

    private String observacao;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
