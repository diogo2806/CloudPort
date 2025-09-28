package br.com.cloudport.servicogate.repository.projection;

import java.time.LocalDate;

public interface TempoMedioPermanenciaProjection {

    LocalDate getDia();

    Double getTempoMedioMinutos();
}
