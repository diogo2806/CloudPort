package br.com.cloudport.servicoyard.patio.dto;

import java.util.List;

public class OpcoesCadastroPatioDto {

    private List<String> statusConteiner;
    private List<String> tiposEquipamento;
    private List<String> statusEquipamento;

    public OpcoesCadastroPatioDto() {
    }

    public OpcoesCadastroPatioDto(List<String> statusConteiner, List<String> tiposEquipamento,
                                  List<String> statusEquipamento) {
        this.statusConteiner = statusConteiner;
        this.tiposEquipamento = tiposEquipamento;
        this.statusEquipamento = statusEquipamento;
    }

    public List<String> getStatusConteiner() {
        return statusConteiner;
    }

    public void setStatusConteiner(List<String> statusConteiner) {
        this.statusConteiner = statusConteiner;
    }

    public List<String> getTiposEquipamento() {
        return tiposEquipamento;
    }

    public void setTiposEquipamento(List<String> tiposEquipamento) {
        this.tiposEquipamento = tiposEquipamento;
    }

    public List<String> getStatusEquipamento() {
        return statusEquipamento;
    }

    public void setStatusEquipamento(List<String> statusEquipamento) {
        this.statusEquipamento = statusEquipamento;
    }
}
