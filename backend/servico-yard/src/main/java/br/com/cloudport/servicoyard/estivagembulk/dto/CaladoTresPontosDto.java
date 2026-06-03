package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class CaladoTresPontosDto {

    private double caladoProaM;
    private double caladoMeioM;
    private double caladoPopaM;
    private double trimM;
    private double listGraus;
    private double pesoCarregadoToneladas;
    private double pesoLightshipToneladas;
    private double pesoBallastToneladas;
    private boolean aprovado;
    private List<ViolacaoEstivaDto> violacoes = new ArrayList<>();

    public static CaladoTresPontosDto calcular(
            double deslocamento, double lpp, double boca, double gm,
            double lcg, double lcb, double pesoTotal, double pesoBallast) {
        CaladoTresPontosDto dto = new CaladoTresPontosDto();
        double cb = 0.75;
        double fatorAgua = 1.025;
        double deslocamentoTotal = deslocamento * 0.35 + pesoTotal + pesoBallast;
        double caladoMedio = deslocamentoTotal / (fatorAgua * lpp * boca * cb);
        double trim = lcg - lcb;
        double caladoPopa = caladoMedio + trim / 2.0;
        double caladoProa = caladoMedio - trim / 2.0;

        dto.caladoMeioM = Math.round(caladoMedio * 1000.0) / 1000.0;
        dto.caladoPopaM = Math.round(caladoPopa * 1000.0) / 1000.0;
        dto.caladoProaM = Math.round(caladoProa * 1000.0) / 1000.0;
        dto.trimM = Math.round(trim * 1000.0) / 1000.0;
        dto.listGraus = Math.round(Math.toDegrees(Math.atan(0 / gm)) * 100.0) / 100.0;
        dto.pesoCarregadoToneladas = Math.round(pesoTotal * 10.0) / 10.0;
        dto.pesoBallastToneladas = Math.round(pesoBallast * 10.0) / 10.0;
        dto.pesoLightshipToneladas = deslocamento * 0.35;
        dto.aprovado = true;
        return dto;
    }

    public double getCaladoProaM() { return caladoProaM; }
    public void setCaladoProaM(double caladoProaM) { this.caladoProaM = caladoProaM; }

    public double getCaladoMeioM() { return caladoMeioM; }
    public void setCaladoMeioM(double caladoMeioM) { this.caladoMeioM = caladoMeioM; }

    public double getCaladoPopaM() { return caladoPopaM; }
    public void setCaladoPopaM(double caladoPopaM) { this.caladoPopaM = caladoPopaM; }

    public double getTrimM() { return trimM; }
    public void setTrimM(double trimM) { this.trimM = trimM; }

    public double getListGraus() { return listGraus; }
    public void setListGraus(double listGraus) { this.listGraus = listGraus; }

    public double getPesoCarregadoToneladas() { return pesoCarregadoToneladas; }
    public void setPesoCarregadoToneladas(double pesoCarregadoToneladas) { this.pesoCarregadoToneladas = pesoCarregadoToneladas; }

    public double getPesoLightshipToneladas() { return pesoLightshipToneladas; }
    public void setPesoLightshipToneladas(double pesoLightshipToneladas) { this.pesoLightshipToneladas = pesoLightshipToneladas; }

    public double getPesoBallastToneladas() { return pesoBallastToneladas; }
    public void setPesoBallastToneladas(double pesoBallastToneladas) { this.pesoBallastToneladas = pesoBallastToneladas; }

    public boolean isAprovado() { return aprovado; }
    public void setAprovado(boolean aprovado) { this.aprovado = aprovado; }

    public List<ViolacaoEstivaDto> getViolacoes() { return violacoes; }
    public void setViolacoes(List<ViolacaoEstivaDto> violacoes) { this.violacoes = violacoes; }
}
