package br.com.cloudport.servicoyard.recursos.dto;

import java.util.List;

public class CalendarioBercoDTO {

    private String codigoBerco;
    private String nomeBerco;
    private List<DiaCalendarioBercoDTO> dias;

    public CalendarioBercoDTO() {
    }

    public CalendarioBercoDTO(String codigoBerco, String nomeBerco, List<DiaCalendarioBercoDTO> dias) {
        this.codigoBerco = codigoBerco;
        this.nomeBerco = nomeBerco;
        this.dias = dias;
    }

    public String getCodigoBerco() {
        return codigoBerco;
    }

    public void setCodigoBerco(String codigoBerco) {
        this.codigoBerco = codigoBerco;
    }

    public String getNomeBerco() {
        return nomeBerco;
    }

    public void setNomeBerco(String nomeBerco) {
        this.nomeBerco = nomeBerco;
    }

    public List<DiaCalendarioBercoDTO> getDias() {
        return dias;
    }

    public void setDias(List<DiaCalendarioBercoDTO> dias) {
        this.dias = dias;
    }
}
