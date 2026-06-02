package br.com.cloudport.servicoyard.recursos.dto;

import java.util.List;

public class ResumoRecursosDTO {

    private int totalBercos;
    private int bercosOperacionais;
    private int bercosEmManutencao;
    private int bercosBloqueados;
    private int reservasConfirmadas;
    private int reservasPropostas;
    private List<ZonaArmazenagemDTO> zonas;
    private List<EquipamentoBercoDTO> equipamentos;
    private List<String> alertas;

    public ResumoRecursosDTO() {
    }

    public int getTotalBercos() {
        return totalBercos;
    }

    public void setTotalBercos(int totalBercos) {
        this.totalBercos = totalBercos;
    }

    public int getBercosOperacionais() {
        return bercosOperacionais;
    }

    public void setBercosOperacionais(int bercosOperacionais) {
        this.bercosOperacionais = bercosOperacionais;
    }

    public int getBercosEmManutencao() {
        return bercosEmManutencao;
    }

    public void setBercosEmManutencao(int bercosEmManutencao) {
        this.bercosEmManutencao = bercosEmManutencao;
    }

    public int getBercosBloqueados() {
        return bercosBloqueados;
    }

    public void setBercosBloqueados(int bercosBloqueados) {
        this.bercosBloqueados = bercosBloqueados;
    }

    public int getReservasConfirmadas() {
        return reservasConfirmadas;
    }

    public void setReservasConfirmadas(int reservasConfirmadas) {
        this.reservasConfirmadas = reservasConfirmadas;
    }

    public int getReservasPropostas() {
        return reservasPropostas;
    }

    public void setReservasPropostas(int reservasPropostas) {
        this.reservasPropostas = reservasPropostas;
    }

    public List<ZonaArmazenagemDTO> getZonas() {
        return zonas;
    }

    public void setZonas(List<ZonaArmazenagemDTO> zonas) {
        this.zonas = zonas;
    }

    public List<EquipamentoBercoDTO> getEquipamentos() {
        return equipamentos;
    }

    public void setEquipamentos(List<EquipamentoBercoDTO> equipamentos) {
        this.equipamentos = equipamentos;
    }

    public List<String> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<String> alertas) {
        this.alertas = alertas;
    }
}
