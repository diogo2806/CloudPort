package br.com.cloudport.servicogate.app.gestor.dto;

import javax.validation.constraints.NotBlank;

public class ResolverDesincroniaRequest {

    @NotBlank(message = "Resolução é obrigatória")
    private String resolucao;

    public String getResolucao() {
        return resolucao;
    }

    public void setResolucao(String resolucao) {
        this.resolucao = resolucao;
    }
}
