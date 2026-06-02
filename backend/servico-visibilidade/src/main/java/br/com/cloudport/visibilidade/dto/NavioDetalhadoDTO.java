package br.com.cloudport.visibilidade.dto;

import java.util.List;

public class NavioDetalhadoDTO {

    private StatusNavioDTO resumo;
    private List<TimelineEventoDTO> timeline;
    private List<AlertaDTO> alertas;
    private String proximaAcao;

    public StatusNavioDTO getResumo() {
        return resumo;
    }

    public void setResumo(StatusNavioDTO resumo) {
        this.resumo = resumo;
    }

    public List<TimelineEventoDTO> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineEventoDTO> timeline) {
        this.timeline = timeline;
    }

    public List<AlertaDTO> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<AlertaDTO> alertas) {
        this.alertas = alertas;
    }

    public String getProximaAcao() {
        return proximaAcao;
    }

    public void setProximaAcao(String proximaAcao) {
        this.proximaAcao = proximaAcao;
    }
}
