package br.com.cloudport.servicogate.dto.relatorio;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.time.LocalDateTime;

public class RelatorioAgendamentoDTO {

    private String codigo;
    private TipoOperacao tipoOperacao;
    private StatusAgendamento status;
    private String transportadora;
    private LocalDateTime horarioPrevistoChegada;
    private LocalDateTime horarioRealChegada;
    private LocalDateTime horarioPrevistoSaida;
    private LocalDateTime horarioRealSaida;

    public static RelatorioAgendamentoDTO fromEntity(Agendamento agendamento) {
        RelatorioAgendamentoDTO dto = new RelatorioAgendamentoDTO();
        dto.setCodigo(agendamento.getCodigo());
        dto.setTipoOperacao(agendamento.getTipoOperacao());
        dto.setStatus(agendamento.getStatus());
        dto.setTransportadora(agendamento.getTransportadora() != null ? agendamento.getTransportadora().getNome() : null);
        dto.setHorarioPrevistoChegada(agendamento.getHorarioPrevistoChegada());
        dto.setHorarioRealChegada(agendamento.getHorarioRealChegada());
        dto.setHorarioPrevistoSaida(agendamento.getHorarioPrevistoSaida());
        dto.setHorarioRealSaida(agendamento.getHorarioRealSaida());
        return dto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public TipoOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacao tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public String getTransportadora() {
        return transportadora;
    }

    public void setTransportadora(String transportadora) {
        this.transportadora = transportadora;
    }

    public LocalDateTime getHorarioPrevistoChegada() {
        return horarioPrevistoChegada;
    }

    public void setHorarioPrevistoChegada(LocalDateTime horarioPrevistoChegada) {
        this.horarioPrevistoChegada = horarioPrevistoChegada;
    }

    public LocalDateTime getHorarioRealChegada() {
        return horarioRealChegada;
    }

    public void setHorarioRealChegada(LocalDateTime horarioRealChegada) {
        this.horarioRealChegada = horarioRealChegada;
    }

    public LocalDateTime getHorarioPrevistoSaida() {
        return horarioPrevistoSaida;
    }

    public void setHorarioPrevistoSaida(LocalDateTime horarioPrevistoSaida) {
        this.horarioPrevistoSaida = horarioPrevistoSaida;
    }

    public LocalDateTime getHorarioRealSaida() {
        return horarioRealSaida;
    }

    public void setHorarioRealSaida(LocalDateTime horarioRealSaida) {
        this.horarioRealSaida = horarioRealSaida;
    }
}
