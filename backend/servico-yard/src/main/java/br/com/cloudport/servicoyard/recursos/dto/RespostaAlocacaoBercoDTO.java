package br.com.cloudport.servicoyard.recursos.dto;

import java.util.List;

public class RespostaAlocacaoBercoDTO {

    private BercoResumoDTO bercoRecomendado;
    private ReservaBercoDTO reservaConfirmada;
    private List<BercoResumoDTO> ranking;
    private List<String> alertas;

    public RespostaAlocacaoBercoDTO() {
    }

    public BercoResumoDTO getBercoRecomendado() {
        return bercoRecomendado;
    }

    public void setBercoRecomendado(BercoResumoDTO bercoRecomendado) {
        this.bercoRecomendado = bercoRecomendado;
    }

    public ReservaBercoDTO getReservaConfirmada() {
        return reservaConfirmada;
    }

    public void setReservaConfirmada(ReservaBercoDTO reservaConfirmada) {
        this.reservaConfirmada = reservaConfirmada;
    }

    public List<BercoResumoDTO> getRanking() {
        return ranking;
    }

    public void setRanking(List<BercoResumoDTO> ranking) {
        this.ranking = ranking;
    }

    public List<String> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<String> alertas) {
        this.alertas = alertas;
    }
}
