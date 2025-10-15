package br.com.cloudport.servicogate.app.transparencia.dto;

import java.time.LocalDate;

public class TempoMedioPermanenciaDTO {

    private LocalDate dia;
    private Double tempoMedioMinutos;

    public TempoMedioPermanenciaDTO(LocalDate dia, Double tempoMedioMinutos) {
        this.dia = dia;
        this.tempoMedioMinutos = tempoMedioMinutos;
    }

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public Double getTempoMedioMinutos() {
        return tempoMedioMinutos;
    }

    public void setTempoMedioMinutos(Double tempoMedioMinutos) {
        this.tempoMedioMinutos = tempoMedioMinutos;
    }
}
