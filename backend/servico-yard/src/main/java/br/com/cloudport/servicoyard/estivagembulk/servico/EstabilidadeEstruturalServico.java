package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EstabilidadeEstruturalServico {

    private static final int N_SECOES = 20;
    private static final double FATOR_AGUA_SALGADA = 1.025;
    private static final double G = 9.81;
    private static final double FATOR_LIGHTSHIP = 0.35;
    private static final double CB = 0.75;

    public EstabilidadeEstrutural calcular(PlanoEstivaBulk plano) {
        NavioGranel navio = plano.getNavio();
        if (navio == null) return EstabilidadeEstrutural.vazia();

        double lpp = navio.getLpp() != null ? navio.getLpp() : 200.0;
        double boca = navio.getBoca() != null ? navio.getBoca() : 32.0;
        double calado = navio.getCalado() != null ? navio.getCalado() : 10.0;
        double deslocamento = navio.getDeslocamento() != null ? navio.getDeslocamento() : 50000.0;
        double deltaL = lpp / N_SECOES;

        double[] pesoSecao = new double[N_SECOES];
        double pesoTotalCarga = 0.0;

        for (PosicaoBobina pos : plano.getPosicoes()) {
            if (pos.getBobina() == null || pos.getPorao() == null) continue;
            double peso = pos.getBobina().getPesoKg() != null ? pos.getBobina().getPesoKg() / 1000.0 : 0.0;
            pesoTotalCarga += peso;
            PoraoNavio porao = pos.getPorao();
            double posLong = porao.getPosLongInicio() != null
                    ? (porao.getPosLongInicio() + porao.getPosLongFim()) / 2.0
                    : lpp / 2.0;
            int secaoIdx = Math.min((int) (posLong / deltaL), N_SECOES - 1);
            pesoSecao[secaoIdx] += peso;
        }

        double pesoLightship = deslocamento * FATOR_LIGHTSHIP;
        double pesoLightshipPorSecao = pesoLightship / N_SECOES;
        for (int i = 0; i < N_SECOES; i++) {
            pesoSecao[i] += pesoLightshipPorSecao;
        }

        double deslocamentoTotal = pesoTotalCarga + pesoLightship;
        double buoyancyPorSecao = deslocamentoTotal / N_SECOES;

        double[] cargaLiquida = new double[N_SECOES];
        for (int i = 0; i < N_SECOES; i++) {
            cargaLiquida[i] = pesoSecao[i] - buoyancyPorSecao;
        }

        double[] sf = new double[N_SECOES + 1];
        sf[0] = 0.0;
        for (int i = 0; i < N_SECOES; i++) {
            sf[i + 1] = sf[i] + cargaLiquida[i] * G;
        }

        double[] bm = new double[N_SECOES + 1];
        bm[0] = 0.0;
        for (int i = 0; i < N_SECOES; i++) {
            bm[i + 1] = bm[i] + sf[i] * deltaL;
        }

        double sfMax = 0.0;
        double bmMax = 0.0;
        for (int i = 0; i <= N_SECOES; i++) {
            if (Math.abs(sf[i]) > Math.abs(sfMax)) sfMax = sf[i];
            if (Math.abs(bm[i]) > Math.abs(bmMax)) bmMax = bm[i];
        }

        double caladoSaida = deslocamentoTotal / (FATOR_AGUA_SALGADA * lpp * boca * CB);

        boolean sagging = bm[N_SECOES / 2] > 0;
        boolean hogging = bm[N_SECOES / 2] < 0;

        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        double bmPermitido = navio.getBmMaxPermitido() != null ? navio.getBmMaxPermitido() : 300000.0;
        double sfPermitido = navio.getSfMaxPermitido() != null ? navio.getSfMaxPermitido() : 60000.0;

        if (Math.abs(bmMax) > bmPermitido) {
            violacoes.add(new ViolacaoEstivaDto("MOMENTO_FLETOR_EXCEDIDO",
                    String.format("BM máx %.0f kN·m excede o permitido %.0f kN·m", Math.abs(bmMax), bmPermitido),
                    null, "PERIGO"));
        } else if (Math.abs(bmMax) > bmPermitido * 0.8) {
            violacoes.add(new ViolacaoEstivaDto("MOMENTO_FLETOR_EXCEDIDO",
                    String.format("BM máx %.0f kN·m está acima de 80%% do limite %.0f kN·m", Math.abs(bmMax), bmPermitido),
                    null, "AVISO"));
        }

        if (Math.abs(sfMax) > sfPermitido) {
            violacoes.add(new ViolacaoEstivaDto("FORCA_CISALHAMENTO_EXCEDIDA",
                    String.format("SF máx %.0f kN excede o permitido %.0f kN", Math.abs(sfMax), sfPermitido),
                    null, "PERIGO"));
        } else if (Math.abs(sfMax) > sfPermitido * 0.8) {
            violacoes.add(new ViolacaoEstivaDto("FORCA_CISALHAMENTO_EXCEDIDA",
                    String.format("SF máx %.0f kN está acima de 80%% do limite %.0f kN", Math.abs(sfMax), sfPermitido),
                    null, "AVISO"));
        }

        if (caladoSaida > calado + 0.5) {
            violacoes.add(new ViolacaoEstivaDto("CALADO_EXCEDIDO",
                    String.format("Calado de saída estimado %.2f m excede o calado de projeto %.2f m", caladoSaida, calado),
                    null, "PERIGO"));
        }

        boolean aprovado = violacoes.stream().noneMatch(v -> "PERIGO".equals(v.getSeveridade()));

        EstabilidadeEstrutural dto = new EstabilidadeEstrutural();
        dto.setBmMaxKnm(Math.round(Math.abs(bmMax) * 10.0) / 10.0);
        dto.setSfMaxKn(Math.round(Math.abs(sfMax) * 10.0) / 10.0);
        dto.setTrimMetros(0.0);
        dto.setCaladoSaidaMetros(Math.round(caladoSaida * 100.0) / 100.0);
        dto.setPesoTotalToneladas(Math.round(pesoTotalCarga * 10.0) / 10.0);
        dto.setHogging(hogging);
        dto.setSagging(sagging);
        dto.setAprovado(aprovado);
        dto.setViolacoes(violacoes);
        return dto;
    }
}
