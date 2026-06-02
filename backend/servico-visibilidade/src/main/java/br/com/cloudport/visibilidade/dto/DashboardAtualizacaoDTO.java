package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class DashboardAtualizacaoDTO {

    private String tipo;
    private LocalDateTime timestamp;
    private DashboardVisibilidadeDTO dashboard;

    public DashboardAtualizacaoDTO() {
    }

    public DashboardAtualizacaoDTO(String tipo, LocalDateTime timestamp, DashboardVisibilidadeDTO dashboard) {
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.dashboard = dashboard;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public DashboardVisibilidadeDTO getDashboard() {
        return dashboard;
    }

    public void setDashboard(DashboardVisibilidadeDTO dashboard) {
        this.dashboard = dashboard;
    }
}
