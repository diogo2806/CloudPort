package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

public class ConfirmacaoChegadaRequest {

    private LocalDateTime dataHoraChegada;
    private String observacao;

    @NotNull
    private Boolean antecipada;

    public LocalDateTime getDataHoraChegada() {
        return dataHoraChegada;
    }

    public void setDataHoraChegada(LocalDateTime dataHoraChegada) {
        this.dataHoraChegada = dataHoraChegada;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Boolean getAntecipada() {
        return antecipada;
    }

    public void setAntecipada(Boolean antecipada) {
        this.antecipada = antecipada;
    }
}
