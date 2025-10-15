package br.com.cloudport.servicogate.app.transparencia.dto;

import java.time.LocalTime;

public class OcupacaoPorHoraDTO {

    private LocalTime horaInicio;
    private Long totalAgendamentos;
    private Integer capacidadeSlot;

    public OcupacaoPorHoraDTO(LocalTime horaInicio, Long totalAgendamentos, Integer capacidadeSlot) {
        this.horaInicio = horaInicio;
        this.totalAgendamentos = totalAgendamentos;
        this.capacidadeSlot = capacidadeSlot;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Long getTotalAgendamentos() {
        return totalAgendamentos;
    }

    public void setTotalAgendamentos(Long totalAgendamentos) {
        this.totalAgendamentos = totalAgendamentos;
    }

    public Integer getCapacidadeSlot() {
        return capacidadeSlot;
    }

    public void setCapacidadeSlot(Integer capacidadeSlot) {
        this.capacidadeSlot = capacidadeSlot;
    }
}
