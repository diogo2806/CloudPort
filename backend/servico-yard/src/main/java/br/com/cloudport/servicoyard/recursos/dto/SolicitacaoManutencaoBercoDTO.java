package br.com.cloudport.servicoyard.recursos.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SolicitacaoManutencaoBercoDTO {

    @NotBlank
    private String bercoCodigo;

    @NotNull
    private LocalDate inicio;

    @NotNull
    private LocalDate fim;

    private String observacao;

    public String getBercoCodigo() {
        return bercoCodigo;
    }

    public void setBercoCodigo(String bercoCodigo) {
        this.bercoCodigo = bercoCodigo;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public void setFim(LocalDate fim) {
        this.fim = fim;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
