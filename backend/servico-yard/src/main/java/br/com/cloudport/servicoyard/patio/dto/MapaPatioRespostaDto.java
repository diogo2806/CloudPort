package br.com.cloudport.servicoyard.patio.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MapaPatioRespostaDto {

    private List<ConteinerMapaDto> conteineres;
    private List<EquipamentoMapaDto> equipamentos;
    private Integer totalLinhas;
    private Integer totalColunas;
    private LocalDateTime atualizadoEm;

    public MapaPatioRespostaDto() {
    }

    public MapaPatioRespostaDto(List<ConteinerMapaDto> conteineres, List<EquipamentoMapaDto> equipamentos,
                                Integer totalLinhas, Integer totalColunas, LocalDateTime atualizadoEm) {
        this.conteineres = conteineres;
        this.equipamentos = equipamentos;
        this.totalLinhas = totalLinhas;
        this.totalColunas = totalColunas;
        this.atualizadoEm = atualizadoEm;
    }

    public List<ConteinerMapaDto> getConteineres() {
        return conteineres;
    }

    public void setConteineres(List<ConteinerMapaDto> conteineres) {
        this.conteineres = conteineres;
    }

    public List<EquipamentoMapaDto> getEquipamentos() {
        return equipamentos;
    }

    public void setEquipamentos(List<EquipamentoMapaDto> equipamentos) {
        this.equipamentos = equipamentos;
    }

    public Integer getTotalLinhas() {
        return totalLinhas;
    }

    public void setTotalLinhas(Integer totalLinhas) {
        this.totalLinhas = totalLinhas;
    }

    public Integer getTotalColunas() {
        return totalColunas;
    }

    public void setTotalColunas(Integer totalColunas) {
        this.totalColunas = totalColunas;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
