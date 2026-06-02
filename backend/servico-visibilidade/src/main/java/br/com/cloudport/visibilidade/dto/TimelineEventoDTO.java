package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class TimelineEventoDTO {

    private String evento;
    private String detalhe;
    private LocalDateTime tempo;

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(String detalhe) {
        this.detalhe = detalhe;
    }

    public LocalDateTime getTempo() {
        return tempo;
    }

    public void setTempo(LocalDateTime tempo) {
        this.tempo = tempo;
    }
}
