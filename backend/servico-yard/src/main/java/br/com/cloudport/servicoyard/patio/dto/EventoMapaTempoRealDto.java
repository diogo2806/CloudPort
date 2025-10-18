package br.com.cloudport.servicoyard.patio.dto;

public class EventoMapaTempoRealDto {

    private String tipoEvento;
    private MapaPatioRespostaDto mapa;

    public EventoMapaTempoRealDto() {
    }

    public EventoMapaTempoRealDto(String tipoEvento, MapaPatioRespostaDto mapa) {
        this.tipoEvento = tipoEvento;
        this.mapa = mapa;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public MapaPatioRespostaDto getMapa() {
        return mapa;
    }

    public void setMapa(MapaPatioRespostaDto mapa) {
        this.mapa = mapa;
    }
}
