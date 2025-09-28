package br.com.cloudport.servicogate.repository.projection;

import java.time.LocalTime;

public interface OcupacaoPorHoraProjection {

    LocalTime getHoraInicio();

    Long getTotalAgendamentos();

    Integer getCapacidadeSlot();
}
