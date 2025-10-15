package br.com.cloudport.servicogate.app.transparencia;

import java.time.LocalTime;

public interface OcupacaoPorHoraProjection {

    LocalTime getHoraInicio();

    Long getTotalAgendamentos();

    Integer getCapacidadeSlot();
}
