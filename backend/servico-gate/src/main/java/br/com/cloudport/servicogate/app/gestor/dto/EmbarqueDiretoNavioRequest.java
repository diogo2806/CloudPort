package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

public class EmbarqueDiretoNavioRequest {

    @NotNull
    private Long agendamentoId;

    @NotNull
    private Long atribuicaoEstivaId;

    private LocalDateTime horarioEmbarque;
    private String operador;
    private String usuario;

    public Long getAgendamentoId() { return agendamentoId; }
    public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
    public Long getAtribuicaoEstivaId() { return atribuicaoEstivaId; }
    public void setAtribuicaoEstivaId(Long atribuicaoEstivaId) { this.atribuicaoEstivaId = atribuicaoEstivaId; }
    public LocalDateTime getHorarioEmbarque() { return horarioEmbarque; }
    public void setHorarioEmbarque(LocalDateTime horarioEmbarque) { this.horarioEmbarque = horarioEmbarque; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
