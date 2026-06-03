package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.BallastOtimizacaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TanqueBallast;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BallastOtimizadorServico {

    private static final double DENSIDADE_AGUA_SALGADA = 1.025;
    private static final double CB = 0.75;
    private static final double FATOR_LIGHTSHIP = 0.35;

    public BallastOtimizacaoDto otimizar(PlanoEstivaBulk plano, double trimAlvoM) {
        BallastOtimizacaoDto resultado = new BallastOtimizacaoDto();
        resultado.setTrimAlvoM(trimAlvoM);

        NavioGranel navio = plano.getNavio();
        if (navio == null || navio.getTanquesBallast() == null || navio.getTanquesBallast().isEmpty()) {
            resultado.setOtimizacaoValida(false);
            resultado.getAlertas().add(new ViolacaoEstivaDto("SEM_TANQUES",
                    "Navio sem tanques de ballast cadastrados", null, "AVISO"));
            return resultado;
        }

        double lpp = navio.getLpp() != null ? navio.getLpp() : 200.0;
        double lcb = lpp / 2.0;

        double pesoCargaToneladas = plano.getPosicoes().stream()
                .mapToDouble(p -> p.getBobina() != null && p.getBobina().getPesoKg() != null
                        ? p.getBobina().getPesoKg() / 1000.0 : 0.0)
                .sum();

        double momentoLongCarga = plano.getPosicoes().stream()
                .mapToDouble(p -> {
                    if (p.getBobina() == null || p.getBobina().getPesoKg() == null) return 0.0;
                    double pos = p.getPorao() != null
                            ? (p.getPorao().getPosLongInicio() + p.getPorao().getPosLongFim()) / 2.0
                            : lcb;
                    return (p.getBobina().getPesoKg() / 1000.0) * pos;
                }).sum();

        double pesoLightship = (navio.getDeslocamento() != null ? navio.getDeslocamento() : 0.0) * FATOR_LIGHTSHIP;
        double lcgSemBallast = (pesoLightship + pesoCargaToneladas) > 0
                ? (pesoLightship * lcb + momentoLongCarga) / (pesoLightship + pesoCargaToneladas)
                : lcb;

        double lcgAlvo = lcb + trimAlvoM;
        double pesoTotalSemBallast = pesoLightship + pesoCargaToneladas;
        double momentoNecessarioBallast = (lcgAlvo * (pesoTotalSemBallast)) - (lcgSemBallast * pesoTotalSemBallast);

        List<TanqueBallast> tanques = navio.getTanquesBallast().stream()
                .sorted(Comparator.comparingDouble(t -> Math.abs(
                        (t.getPosLongCentroM() != null ? t.getPosLongCentroM() : lcb) - lcb)))
                .toList();

        List<BallastOtimizacaoDto.TanqueBallastResultadoDto> resultadosTanques = new ArrayList<>();
        double pesoBallastTotal = 0.0;
        double momentoLongBallastTotal = 0.0;
        double momentoTransBallastTotal = 0.0;

        for (TanqueBallast tanque : tanques) {
            BallastOtimizacaoDto.TanqueBallastResultadoDto r = new BallastOtimizacaoDto.TanqueBallastResultadoDto();
            r.setTanqueId(tanque.getId());
            r.setNomeTanque(tanque.getNome());
            r.setCapacidadeM3(tanque.getCapacidadeM3());
            r.setVolumeAnteriorM3(tanque.getVolumeAtualM3() != null ? tanque.getVolumeAtualM3() : 0.0);

            double posLong = tanque.getPosLongCentroM() != null ? tanque.getPosLongCentroM() : lcb;
            double contribuicaoMomento = posLong - lcgAlvo;

            double volumeIdeal = 0.0;
            if (Math.abs(momentoNecessarioBallast) > 0 && Math.abs(contribuicaoMomento) > 0.1) {
                double pesoNecessario = momentoNecessarioBallast / contribuicaoMomento;
                if (pesoNecessario > 0) {
                    volumeIdeal = Math.min(pesoNecessario / DENSIDADE_AGUA_SALGADA, tanque.getCapacidadeM3());
                    momentoNecessarioBallast -= (volumeIdeal * DENSIDADE_AGUA_SALGADA) * contribuicaoMomento;
                }
            }

            r.setVolumeRecomendadoM3(Math.round(volumeIdeal * 100.0) / 100.0);
            r.setPercentualEnchimento(tanque.getCapacidadeM3() > 0
                    ? Math.round(volumeIdeal / tanque.getCapacidadeM3() * 1000.0) / 10.0 : 0.0);

            double pesoTanque = volumeIdeal * DENSIDADE_AGUA_SALGADA;
            pesoBallastTotal += pesoTanque;
            momentoLongBallastTotal += pesoTanque * posLong;
            if (tanque.getPosTransCentroM() != null) {
                momentoTransBallastTotal += pesoTanque * tanque.getPosTransCentroM();
            }

            resultadosTanques.add(r);
        }

        resultado.setTanques(resultadosTanques);
        resultado.setPesoTotalBallastToneladas(Math.round(pesoBallastTotal * 10.0) / 10.0);

        double pesoTotalFinal = pesoLightship + pesoCargaToneladas + pesoBallastTotal;
        double lcgFinal = pesoTotalFinal > 0
                ? (pesoLightship * lcb + momentoLongCarga + momentoLongBallastTotal) / pesoTotalFinal
                : lcb;
        double tcgFinal = pesoTotalFinal > 0 ? momentoTransBallastTotal / pesoTotalFinal : 0.0;
        double gm = navio.getGm() != null ? navio.getGm() : 1.5;

        resultado.setTrimResultanteM(Math.round((lcgFinal - lcb) * 1000.0) / 1000.0);
        resultado.setListResultanteGraus(Math.round(Math.toDegrees(Math.atan(tcgFinal / gm)) * 100.0) / 100.0);
        resultado.setOtimizacaoValida(true);

        return resultado;
    }
}
