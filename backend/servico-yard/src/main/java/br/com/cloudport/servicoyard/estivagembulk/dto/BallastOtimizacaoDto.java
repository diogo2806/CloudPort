package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class BallastOtimizacaoDto {

    private double trimAlvoM;
    private double trimResultanteM;
    private double listResultanteGraus;
    private double pesoTotalBallastToneladas;
    private boolean otimizacaoValida;
    private List<TanqueBallastResultadoDto> tanques = new ArrayList<>();
    private List<ViolacaoEstivaDto> alertas = new ArrayList<>();

    public static class TanqueBallastResultadoDto {
        private Long tanqueId;
        private String nomeTanque;
        private double volumeAnteriorM3;
        private double volumeRecomendadoM3;
        private double capacidadeM3;
        private double percentualEnchimento;

        public Long getTanqueId() { return tanqueId; }
        public void setTanqueId(Long tanqueId) { this.tanqueId = tanqueId; }
        public String getNomeTanque() { return nomeTanque; }
        public void setNomeTanque(String nomeTanque) { this.nomeTanque = nomeTanque; }
        public double getVolumeAnteriorM3() { return volumeAnteriorM3; }
        public void setVolumeAnteriorM3(double volumeAnteriorM3) { this.volumeAnteriorM3 = volumeAnteriorM3; }
        public double getVolumeRecomendadoM3() { return volumeRecomendadoM3; }
        public void setVolumeRecomendadoM3(double volumeRecomendadoM3) { this.volumeRecomendadoM3 = volumeRecomendadoM3; }
        public double getCapacidadeM3() { return capacidadeM3; }
        public void setCapacidadeM3(double capacidadeM3) { this.capacidadeM3 = capacidadeM3; }
        public double getPercentualEnchimento() { return percentualEnchimento; }
        public void setPercentualEnchimento(double percentualEnchimento) { this.percentualEnchimento = percentualEnchimento; }
    }

    public double getTrimAlvoM() { return trimAlvoM; }
    public void setTrimAlvoM(double trimAlvoM) { this.trimAlvoM = trimAlvoM; }
    public double getTrimResultanteM() { return trimResultanteM; }
    public void setTrimResultanteM(double trimResultanteM) { this.trimResultanteM = trimResultanteM; }
    public double getListResultanteGraus() { return listResultanteGraus; }
    public void setListResultanteGraus(double listResultanteGraus) { this.listResultanteGraus = listResultanteGraus; }
    public double getPesoTotalBallastToneladas() { return pesoTotalBallastToneladas; }
    public void setPesoTotalBallastToneladas(double pesoTotalBallastToneladas) { this.pesoTotalBallastToneladas = pesoTotalBallastToneladas; }
    public boolean isOtimizacaoValida() { return otimizacaoValida; }
    public void setOtimizacaoValida(boolean otimizacaoValida) { this.otimizacaoValida = otimizacaoValida; }
    public List<TanqueBallastResultadoDto> getTanques() { return tanques; }
    public void setTanques(List<TanqueBallastResultadoDto> tanques) { this.tanques = tanques; }
    public List<ViolacaoEstivaDto> getAlertas() { return alertas; }
    public void setAlertas(List<ViolacaoEstivaDto> alertas) { this.alertas = alertas; }
}
