package br.com.cloudport.servicoautenticacao.app.notificacoes.dto;

import javax.validation.constraints.NotNull;

public class AtualizacaoStatusCanalDTO {

    @NotNull(message = "Informe o status desejado para o canal.")
    private Boolean habilitado;

    public AtualizacaoStatusCanalDTO() {
    }

    public Boolean getHabilitado() {
        return habilitado;
    }

    public void setHabilitado(Boolean habilitado) {
        this.habilitado = habilitado;
    }
}
