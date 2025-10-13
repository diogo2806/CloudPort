package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;

public class AgendamentoStatusEventDTO {

    private Long agendamentoId;
    private String status;
    private String statusDescricao;
    private LocalDateTime horarioRealChegada;
    private LocalDateTime horarioRealSaida;
    private String observacao;

    public AgendamentoStatusEventDTO() {
    }

    public AgendamentoStatusEventDTO(Long agendamentoId, String status, String statusDescricao,
                                     LocalDateTime horarioRealChegada, LocalDateTime horarioRealSaida,
                                     String observacao) {
        this.agendamentoId = agendamentoId;
        this.status = status;
        this.statusDescricao = statusDescricao;
        this.horarioRealChegada = horarioRealChegada;
        this.horarioRealSaida = horarioRealSaida;
        this.observacao = observacao;
    }

    public Long getAgendamentoId() {
        return agendamentoId;
    }

    public void setAgendamentoId(Long agendamentoId) {
        this.agendamentoId = agendamentoId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescricao() {
        return statusDescricao;
    }

    public void setStatusDescricao(String statusDescricao) {
        this.statusDescricao = statusDescricao;
    }

    public LocalDateTime getHorarioRealChegada() {
        return horarioRealChegada;
    }

    public void setHorarioRealChegada(LocalDateTime horarioRealChegada) {
        this.horarioRealChegada = horarioRealChegada;
    }

    public LocalDateTime getHorarioRealSaida() {
        return horarioRealSaida;
    }

    public void setHorarioRealSaida(LocalDateTime horarioRealSaida) {
        this.horarioRealSaida = horarioRealSaida;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
