package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;

public class JanelaLembreteDTO {

    private Long agendamentoId;
    private String codigoAgendamento;
    private LocalDateTime horarioPrevistoChegada;
    private LocalDateTime horarioPrevistoSaida;
    private long minutosRestantes;

    public JanelaLembreteDTO() {
    }

    public JanelaLembreteDTO(Long agendamentoId, String codigoAgendamento,
                             LocalDateTime horarioPrevistoChegada, LocalDateTime horarioPrevistoSaida,
                             long minutosRestantes) {
        this.agendamentoId = agendamentoId;
        this.codigoAgendamento = codigoAgendamento;
        this.horarioPrevistoChegada = horarioPrevistoChegada;
        this.horarioPrevistoSaida = horarioPrevistoSaida;
        this.minutosRestantes = minutosRestantes;
    }

    public Long getAgendamentoId() {
        return agendamentoId;
    }

    public void setAgendamentoId(Long agendamentoId) {
        this.agendamentoId = agendamentoId;
    }

    public String getCodigoAgendamento() {
        return codigoAgendamento;
    }

    public void setCodigoAgendamento(String codigoAgendamento) {
        this.codigoAgendamento = codigoAgendamento;
    }

    public LocalDateTime getHorarioPrevistoChegada() {
        return horarioPrevistoChegada;
    }

    public void setHorarioPrevistoChegada(LocalDateTime horarioPrevistoChegada) {
        this.horarioPrevistoChegada = horarioPrevistoChegada;
    }

    public LocalDateTime getHorarioPrevistoSaida() {
        return horarioPrevistoSaida;
    }

    public void setHorarioPrevistoSaida(LocalDateTime horarioPrevistoSaida) {
        this.horarioPrevistoSaida = horarioPrevistoSaida;
    }

    public long getMinutosRestantes() {
        return minutosRestantes;
    }

    public void setMinutosRestantes(long minutosRestantes) {
        this.minutosRestantes = minutosRestantes;
    }
}
