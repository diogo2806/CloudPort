package br.com.cloudport.servicoyard.patio.dto;

import java.util.List;

public class MapaPatioFiltro {

    private List<String> status;
    private List<String> tiposCarga;
    private List<String> destinos;
    private List<String> camadasOperacionais;
    private List<String> tiposEquipamento;

    public MapaPatioFiltro(List<String> status, List<String> tiposCarga, List<String> destinos,
                           List<String> camadasOperacionais, List<String> tiposEquipamento) {
        this.status = status;
        this.tiposCarga = tiposCarga;
        this.destinos = destinos;
        this.camadasOperacionais = camadasOperacionais;
        this.tiposEquipamento = tiposEquipamento;
    }

    public List<String> getStatus() {
        return status;
    }

    public List<String> getTiposCarga() {
        return tiposCarga;
    }

    public List<String> getDestinos() {
        return destinos;
    }

    public List<String> getCamadasOperacionais() {
        return camadasOperacionais;
    }

    public List<String> getTiposEquipamento() {
        return tiposEquipamento;
    }
}
