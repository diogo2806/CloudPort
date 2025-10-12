package br.com.cloudport.servicogate.dto.dashboard;

import java.util.List;

public class DashboardResumoDTO {

    private long totalAgendamentos;
    private double percentualPontualidade;
    private double percentualNoShow;
    private double percentualOcupacaoSlots;
    private double tempoMedioTurnaroundMinutos;
    private List<OcupacaoPorHoraDTO> ocupacaoPorHora;
    private List<TempoMedioPermanenciaDTO> turnaroundPorDia;

    public long getTotalAgendamentos() {
        return totalAgendamentos;
    }

    public void setTotalAgendamentos(long totalAgendamentos) {
        this.totalAgendamentos = totalAgendamentos;
    }

    public double getPercentualPontualidade() {
        return percentualPontualidade;
    }

    public void setPercentualPontualidade(double percentualPontualidade) {
        this.percentualPontualidade = percentualPontualidade;
    }

    public double getPercentualNoShow() {
        return percentualNoShow;
    }

    public void setPercentualNoShow(double percentualNoShow) {
        this.percentualNoShow = percentualNoShow;
    }

    public double getPercentualOcupacaoSlots() {
        return percentualOcupacaoSlots;
    }

    public void setPercentualOcupacaoSlots(double percentualOcupacaoSlots) {
        this.percentualOcupacaoSlots = percentualOcupacaoSlots;
    }

    public double getTempoMedioTurnaroundMinutos() {
        return tempoMedioTurnaroundMinutos;
    }

    public void setTempoMedioTurnaroundMinutos(double tempoMedioTurnaroundMinutos) {
        this.tempoMedioTurnaroundMinutos = tempoMedioTurnaroundMinutos;
    }

    public List<OcupacaoPorHoraDTO> getOcupacaoPorHora() {
        return ocupacaoPorHora;
    }

    public void setOcupacaoPorHora(List<OcupacaoPorHoraDTO> ocupacaoPorHora) {
        this.ocupacaoPorHora = ocupacaoPorHora;
    }

    public List<TempoMedioPermanenciaDTO> getTurnaroundPorDia() {
        return turnaroundPorDia;
    }

    public void setTurnaroundPorDia(List<TempoMedioPermanenciaDTO> turnaroundPorDia) {
        this.turnaroundPorDia = turnaroundPorDia;
    }
}
