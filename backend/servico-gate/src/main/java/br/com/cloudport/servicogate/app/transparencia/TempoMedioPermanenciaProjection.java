package br.com.cloudport.servicogate.app.transparencia;

import java.time.LocalDate;

public interface TempoMedioPermanenciaProjection {

    LocalDate getDia();

    Double getTempoMedioMinutos();
}
