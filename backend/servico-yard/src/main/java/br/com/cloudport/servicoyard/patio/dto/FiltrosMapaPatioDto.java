package br.com.cloudport.servicoyard.patio.dto;

import java.util.List;

public class FiltrosMapaPatioDto {

    private List<String> statusDisponiveis;
    private List<String> tiposCargaDisponiveis;
    private List<String> destinosDisponiveis;
    private List<String> camadasOperacionaisDisponiveis;
    private List<String> tiposEquipamentoDisponiveis;

    public FiltrosMapaPatioDto() {
    }

    public FiltrosMapaPatioDto(List<String> statusDisponiveis, List<String> tiposCargaDisponiveis,
                               List<String> destinosDisponiveis, List<String> camadasOperacionaisDisponiveis,
                               List<String> tiposEquipamentoDisponiveis) {
        this.statusDisponiveis = statusDisponiveis;
        this.tiposCargaDisponiveis = tiposCargaDisponiveis;
        this.destinosDisponiveis = destinosDisponiveis;
        this.camadasOperacionaisDisponiveis = camadasOperacionaisDisponiveis;
        this.tiposEquipamentoDisponiveis = tiposEquipamentoDisponiveis;
    }

    public List<String> getStatusDisponiveis() {
        return statusDisponiveis;
    }

    public void setStatusDisponiveis(List<String> statusDisponiveis) {
        this.statusDisponiveis = statusDisponiveis;
    }

    public List<String> getTiposCargaDisponiveis() {
        return tiposCargaDisponiveis;
    }

    public void setTiposCargaDisponiveis(List<String> tiposCargaDisponiveis) {
        this.tiposCargaDisponiveis = tiposCargaDisponiveis;
    }

    public List<String> getDestinosDisponiveis() {
        return destinosDisponiveis;
    }

    public void setDestinosDisponiveis(List<String> destinosDisponiveis) {
        this.destinosDisponiveis = destinosDisponiveis;
    }

    public List<String> getCamadasOperacionaisDisponiveis() {
        return camadasOperacionaisDisponiveis;
    }

    public void setCamadasOperacionaisDisponiveis(List<String> camadasOperacionaisDisponiveis) {
        this.camadasOperacionaisDisponiveis = camadasOperacionaisDisponiveis;
    }

    public List<String> getTiposEquipamentoDisponiveis() {
        return tiposEquipamentoDisponiveis;
    }

    public void setTiposEquipamentoDisponiveis(List<String> tiposEquipamentoDisponiveis) {
        this.tiposEquipamentoDisponiveis = tiposEquipamentoDisponiveis;
    }
}
