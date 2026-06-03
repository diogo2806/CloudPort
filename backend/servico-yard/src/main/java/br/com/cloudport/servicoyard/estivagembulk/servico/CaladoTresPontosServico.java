package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.CaladoTresPontosDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TanqueBallast;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CaladoTresPontosServico {

    private static final double CB = 0.75;
    private static final double FATOR_AGUA_SALGADA = 1.025;
    private static final double FATOR_LIGHTSHIP = 0.35;
    private static final double MAX_TRIM_M = 3.0;

    public CaladoTresPontosDto calcular(PlanoEstivaBulk plano) {
        CaladoTresPontosDto dto = new CaladoTresPontosDto();
        NavioGranel navio = plano.getNavio();

        if (navio == null) {
            dto.setAprovado(true);
            return dto;
        }

        double lpp = navio.getLpp() != null ? navio.getLpp() : 0.0;
        double boca = navio.getBoca() != null ? navio.getBoca() : 0.0;
        double deslocamento = navio.getDeslocamento() != null ? navio.getDeslocamento() : 0.0;
        double gm = navio.getGm() != null ? navio.getGm() : 1.5;
        double lcb = lpp / 2.0;

        double pesoCargaToneladas = plano.getPosicoes().stream()
                .mapToDouble(p -> p.getBobina() != null && p.getBobina().getPesoKg() != null
                        ? p.getBobina().getPesoKg() / 1000.0 : 0.0)
                .sum();

        double momentoLongCarga = plano.getPosicoes().stream()
                .mapToDouble(p -> {
                    if (p.getBobina() == null || p.getBobina().getPesoKg() == null) return 0.0;
                    double pesoT = p.getBobina().getPesoKg() / 1000.0;
                    double posLong = p.getPorao() != null
                            ? (p.getPorao().getPosLongInicio() + p.getPorao().getPosLongFim()) / 2.0
                            : lpp / 2.0;
                    return pesoT * posLong;
                }).sum();

        double momentoTransCarga = plano.getPosicoes().stream()
                .mapToDouble(p -> {
                    if (p.getBobina() == null || p.getBobina().getPesoKg() == null) return 0.0;
                    double pesoT = p.getBobina().getPesoKg() / 1000.0;
                    double posTrans = p.getPosicaoY() != null ? p.getPosicaoY() : 0.0;
                    return pesoT * posTrans;
                }).sum();

        double pesoBallastToneladas = 0.0;
        double momentoLongBallast = 0.0;
        double momentoTransBallast = 0.0;

        if (navio.getTanquesBallast() != null) {
            for (TanqueBallast t : navio.getTanquesBallast()) {
                double peso = t.getPesoToneladas();
                pesoBallastToneladas += peso;
                if (t.getPosLongCentroM() != null) momentoLongBallast += peso * t.getPosLongCentroM();
                if (t.getPosTransCentroM() != null) momentoTransBallast += peso * t.getPosTransCentroM();
            }
        }

        double pesoLightship = deslocamento * FATOR_LIGHTSHIP;
        double pesoTotal = pesoLightship + pesoCargaToneladas + pesoBallastToneladas;

        double lcg = pesoTotal > 0
                ? (pesoLightship * lcb + momentoLongCarga + momentoLongBallast) / pesoTotal
                : lcb;
        double tcg = pesoTotal > 0
                ? (momentoTransCarga + momentoTransBallast) / pesoTotal
                : 0.0;

        double caladoMedio = (lpp > 0 && boca > 0)
                ? pesoTotal / (FATOR_AGUA_SALGADA * lpp * boca * CB)
                : 0.0;
        double trim = lcg - lcb;
        double list = gm != 0 ? Math.toDegrees(Math.atan(tcg / gm)) : 0.0;

        double caladoPopa = caladoMedio + trim / 2.0;
        double caladoProa = caladoMedio - trim / 2.0;

        dto.setCaladoMeioM(round3(caladoMedio));
        dto.setCaladoPopaM(round3(caladoPopa));
        dto.setCaladoProaM(round3(caladoProa));
        dto.setTrimM(round3(trim));
        dto.setListGraus(round2(list));
        dto.setPesoCarregadoToneladas(round1(pesoCargaToneladas));
        dto.setPesoLightshipToneladas(round1(pesoLightship));
        dto.setPesoBallastToneladas(round1(pesoBallastToneladas));

        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();

        if (Math.abs(trim) > MAX_TRIM_M) {
            violacoes.add(new ViolacaoEstivaDto("TRIM_EXCESSIVO",
                    String.format("Trim de %.2fm excede limite de %.1fm", trim, MAX_TRIM_M),
                    null, "PERIGO"));
        }

        if (navio.getCalado() != null && caladoPopa > navio.getCalado() * 1.05) {
            violacoes.add(new ViolacaoEstivaDto("CALADO_MAXIMO_EXCEDIDO",
                    String.format("Calado de popa %.3fm excede calado máximo do navio %.1fm", caladoPopa, navio.getCalado()),
                    null, "PERIGO"));
        }

        dto.setViolacoes(violacoes);
        dto.setAprovado(violacoes.stream().noneMatch(v -> "PERIGO".equals(v.getSeveridade())));
        return dto;
    }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
