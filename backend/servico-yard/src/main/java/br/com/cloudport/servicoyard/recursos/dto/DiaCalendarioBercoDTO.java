package br.com.cloudport.servicoyard.recursos.dto;

import java.time.LocalDate;

public class DiaCalendarioBercoDTO {

    private LocalDate data;
    private String status;
    private String rotulo;
    private String navioCodigo;
    private String navioNome;
    private Long reservaId;

    public DiaCalendarioBercoDTO() {
    }

    public DiaCalendarioBercoDTO(LocalDate data, String status, String rotulo, String navioCodigo, String navioNome, Long reservaId) {
        this.data = data;
        this.status = status;
        this.rotulo = rotulo;
        this.navioCodigo = navioCodigo;
        this.navioNome = navioNome;
        this.reservaId = reservaId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRotulo() {
        return rotulo;
    }

    public void setRotulo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getNavioCodigo() {
        return navioCodigo;
    }

    public void setNavioCodigo(String navioCodigo) {
        this.navioCodigo = navioCodigo;
    }

    public String getNavioNome() {
        return navioNome;
    }

    public void setNavioNome(String navioNome) {
        this.navioNome = navioNome;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public void setReservaId(Long reservaId) {
        this.reservaId = reservaId;
    }
}
