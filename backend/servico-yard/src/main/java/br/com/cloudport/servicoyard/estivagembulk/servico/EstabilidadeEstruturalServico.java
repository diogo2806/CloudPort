package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class EstabilidadeEstruturalServico {

    private static final double ACELERACAO_GRAVIDADE = 9.80665;
    private static final double TOLERANCIA_DISTRIBUICAO = 0.02;

    public EstabilidadeEstrutural calcular(PlanoEstivaBulk plano) {
        EstabilidadeEstrutural dto = EstabilidadeEstrutural.vazia();
        NavioGranel navio = plano.getNavio();
        List<PosicaoBobina> posicoes = plano.getPosicoes() != null ? plano.getPosicoes() : List.of();
        double pesoCargaToneladas = calcularPesoCargaToneladas(posicoes);
        dto.setPesoTotalToneladas(arredondar(pesoCargaToneladas, 1));

        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        List<String> dadosAusentes = validarDadosOperacionais(navio, posicoes);
        if (!dadosAusentes.isEmpty()) {
            violacoes.add(new ViolacaoEstivaDto(
                    "DADOS_ESTABILIDADE_INCOMPLETOS",
                    "Cálculo identificado como simulação não operacional. Dados obrigatórios ausentes ou inválidos: "
                            + String.join(", ", dadosAusentes),
                    null,
                    "PERIGO"));
            dto.setVersaoDadosHidrostaticos(navio != null ? navio.getVersaoDadosHidrostaticos() : null);
            dto.setVersaoDadosEstruturais(navio != null ? navio.getVersaoDadosEstruturais() : null);
            dto.setOperacional(false);
            dto.setAprovado(false);
            dto.setMemoriaCalculo("SIMULACAO_NAO_OPERACIONAL; aprovação bloqueada; hidro="
                    + valorOuAusente(navio != null ? navio.getVersaoDadosHidrostaticos() : null)
                    + "; estrutural="
                    + valorOuAusente(navio != null ? navio.getVersaoDadosEstruturais() : null));
            dto.setViolacoes(violacoes);
            return dto;
        }

        List<Double> posicoesSecoes = parseSerie(navio.getPosicoesSecoes());
        List<Double> pesoLeveSecoes = parseSerie(navio.getPesoLeveSecoes());
        List<Double> empuxoSecoes = parseSerie(navio.getEmpuxoSecoes());
        List<Double> limitesSfSecoes = parseSerie(navio.getLimitesSfSecoes());
        List<Double> limitesBmSecoes = parseSerie(navio.getLimitesBmSecoes());

        double pesoLastroToneladas = valorOuZero(navio.getPesoLastroToneladas());
        double pesoTotalToneladas = navio.getPesoLeveToneladas() + pesoLastroToneladas + pesoCargaToneladas;
        double momentoLongitudinal = navio.getPesoLeveToneladas() * navio.getLcgPesoLeve();
        double momentoTransversal = navio.getPesoLeveToneladas() * navio.getTcgPesoLeve();
        double momentoVertical = navio.getPesoLeveToneladas() * navio.getVcgPesoLeve();

        if (pesoLastroToneladas > 0.0) {
            momentoLongitudinal += pesoLastroToneladas * navio.getLcgLastro();
            momentoTransversal += pesoLastroToneladas * navio.getTcgLastro();
            momentoVertical += pesoLastroToneladas * navio.getVcgLastro();
        }

        for (PosicaoBobina posicao : posicoes) {
            double pesoToneladas = posicao.getBobina().getPesoKg() / 1000.0;
            momentoLongitudinal += pesoToneladas * calcularPosicaoLongitudinal(posicao);
            momentoTransversal += pesoToneladas * posicao.getPosicaoY();
            momentoVertical += pesoToneladas * calcularPosicaoVertical(posicao);
        }

        double lcg = momentoLongitudinal / pesoTotalToneladas;
        double tcg = momentoTransversal / pesoTotalToneladas;
        double vcg = momentoVertical / pesoTotalToneladas;
        double gm = navio.getKm() - vcg;
        double calado = navio.getCalado()
                + (pesoTotalToneladas - navio.getDeslocamento()) / (navio.getTpc() * 100.0);
        double trim = ((lcg - navio.getLcb()) * pesoTotalToneladas) / (navio.getMct1cm() * 100.0);
        double banda = gm != 0.0 ? Math.toDegrees(Math.atan(tcg / gm)) : 90.0;

        validarCoerenciaDistribuicao(navio, pesoLeveSecoes, empuxoSecoes, violacoes);
        ResultadoLongitudinal longitudinal = calcularEsforcosLongitudinais(
                posicoes,
                navio,
                posicoesSecoes,
                pesoLeveSecoes,
                empuxoSecoes,
                limitesSfSecoes,
                limitesBmSecoes,
                pesoTotalToneladas,
                violacoes);

        verificarCalado(calado, navio.getCaladoMaximo(), violacoes);
        verificarTrim(trim, navio.getTrimMaximo(), violacoes);
        verificarBanda(banda, navio.getBandaMaxima(), violacoes);
        verificarGm(gm, navio.getGmMinimo(), violacoes);
        verificarLimiteGlobal("FORCA_CISALHAMENTO", longitudinal.sfMaxKn,
                navio.getSfMaxPermitido(), violacoes);
        verificarLimiteGlobal("MOMENTO_FLETOR", longitudinal.bmMaxKnm,
                navio.getBmMaxPermitido(), violacoes);

        boolean aprovado = violacoes.stream()
                .noneMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));

        dto.setBmMaxKnm(arredondar(longitudinal.bmMaxKnm, 1));
        dto.setSfMaxKn(arredondar(longitudinal.sfMaxKn, 1));
        dto.setTrimMetros(arredondar(trim, 2));
        dto.setListGraus(arredondar(banda, 2));
        dto.setGmMetros(arredondar(gm, 3));
        dto.setCaladoSaidaMetros(arredondar(calado, 2));
        dto.setPesoTotalToneladas(arredondar(pesoTotalToneladas, 1));
        dto.setHogging(longitudinal.bmAssinadoNoMaximo < 0.0);
        dto.setSagging(longitudinal.bmAssinadoNoMaximo > 0.0);
        dto.setOperacional(true);
        dto.setAprovado(aprovado);
        dto.setVersaoDadosHidrostaticos(navio.getVersaoDadosHidrostaticos());
        dto.setVersaoDadosEstruturais(navio.getVersaoDadosEstruturais());
        dto.setMemoriaCalculo(criarMemoriaCalculo(navio, dto));
        dto.setViolacoes(violacoes);
        return dto;
    }

    private List<String> validarDadosOperacionais(NavioGranel navio, List<PosicaoBobina> posicoes) {
        List<String> ausentes = new ArrayList<>();
        if (navio == null) {
            ausentes.add("navio");
            return ausentes;
        }
        exigirTexto(navio.getVersaoDadosHidrostaticos(), "versaoDadosHidrostaticos", ausentes);
        exigirTexto(navio.getVersaoDadosEstruturais(), "versaoDadosEstruturais", ausentes);
        exigirPositivo(navio.getLpp(), "lpp", ausentes);
        exigirPositivo(navio.getBoca(), "boca", ausentes);
        exigirPositivo(navio.getCalado(), "caladoReferencia", ausentes);
        exigirPositivo(navio.getDeslocamento(), "deslocamentoReferencia", ausentes);
        exigirPositivo(navio.getTpc(), "tpc", ausentes);
        exigirNumero(navio.getLcb(), "lcb", ausentes);
        exigirPositivo(navio.getKm(), "km", ausentes);
        exigirPositivo(navio.getMct1cm(), "mct1cm", ausentes);
        exigirPositivo(navio.getCaladoMaximo(), "caladoMaximo", ausentes);
        exigirPositivo(navio.getTrimMaximo(), "trimMaximo", ausentes);
        exigirPositivo(navio.getBandaMaxima(), "bandaMaxima", ausentes);
        exigirPositivo(navio.getGmMinimo(), "gmMinimo", ausentes);
        exigirPositivo(navio.getPesoLeveToneladas(), "pesoLeveToneladas", ausentes);
        exigirNumero(navio.getLcgPesoLeve(), "lcgPesoLeve", ausentes);
        exigirNumero(navio.getTcgPesoLeve(), "tcgPesoLeve", ausentes);
        exigirNumero(navio.getVcgPesoLeve(), "vcgPesoLeve", ausentes);
        exigirPositivo(navio.getBmMaxPermitido(), "bmMaxPermitido", ausentes);
        exigirPositivo(navio.getSfMaxPermitido(), "sfMaxPermitido", ausentes);

        if (valorOuZero(navio.getPesoLastroToneladas()) > 0.0) {
            exigirNumero(navio.getLcgLastro(), "lcgLastro", ausentes);
            exigirNumero(navio.getTcgLastro(), "tcgLastro", ausentes);
            exigirNumero(navio.getVcgLastro(), "vcgLastro", ausentes);
        }

        validarSeries(navio.getPosicoesSecoes(), navio.getPesoLeveSecoes(), navio.getEmpuxoSecoes(),
                navio.getLimitesSfSecoes(), navio.getLimitesBmSecoes(), ausentes);

        if (posicoes.isEmpty()) {
            ausentes.add("bobinasPosicionadas");
        }
        for (PosicaoBobina posicao : posicoes) {
            if (posicao.getBobina() == null
                    || posicao.getBobina().getPesoKg() == null
                    || posicao.getBobina().getPesoKg() <= 0.0) {
                ausentes.add("peso da bobina na posição " + identificarPosicao(posicao));
            }
            if (posicao.getPorao() == null
                    || posicao.getPorao().getPosLongInicio() == null
                    || posicao.getPorao().getPosLongFim() == null) {
                ausentes.add("coordenadas longitudinais do porão na posição " + identificarPosicao(posicao));
            }
            if (posicao.getPosicaoX() == null || posicao.getPosicaoY() == null) {
                ausentes.add("coordenadas físicas da bobina na posição " + identificarPosicao(posicao));
            }
            if (posicao.getBobina() == null
                    || posicao.getBobina().getDiametroExternoMm() == null
                    || posicao.getBobina().getDiametroExternoMm() <= 0.0
                    || posicao.getEspessuraDunnageMm() == null
                    || posicao.getCamada() <= 0) {
                ausentes.add("dados verticais da bobina na posição " + identificarPosicao(posicao));
            }
            if (posicao.getPorao() != null
                    && posicao.getPorao().getPosLongInicio() != null
                    && posicao.getPorao().getPosLongFim() != null
                    && posicao.getPosicaoX() != null) {
                double posicaoLongitudinal = calcularPosicaoLongitudinal(posicao);
                if (posicaoLongitudinal < posicao.getPorao().getPosLongInicio()
                        || posicaoLongitudinal > posicao.getPorao().getPosLongFim()) {
                    ausentes.add("posição longitudinal fora do porão " + identificarPosicao(posicao));
                }
            }
        }
        return ausentes;
    }

    private void validarSeries(String posicoes, String pesoLeve, String empuxo, String limitesSf,
            String limitesBm, List<String> ausentes) {
        List<Double> seriePosicoes = parseSerie(posicoes);
        List<Double> seriePeso = parseSerie(pesoLeve);
        List<Double> serieEmpuxo = parseSerie(empuxo);
        List<Double> serieSf = parseSerie(limitesSf);
        List<Double> serieBm = parseSerie(limitesBm);
        int quantidade = seriePosicoes.size();
        if (quantidade < 2
                || seriePeso.size() != quantidade
                || serieEmpuxo.size() != quantidade
                || serieSf.size() != quantidade
                || serieBm.size() != quantidade) {
            ausentes.add("séries estruturais versionadas com o mesmo número de seções");
            return;
        }
        for (int i = 1; i < quantidade; i++) {
            if (seriePosicoes.get(i) <= seriePosicoes.get(i - 1)) {
                ausentes.add("posições de seções estritamente crescentes");
                break;
            }
        }
        if (serieEmpuxo.stream().mapToDouble(Double::doubleValue).sum() <= 0.0) {
            ausentes.add("distribuição de empuxo positiva");
        }
        if (serieSf.stream().anyMatch(valor -> valor == null || valor <= 0.0)) {
            ausentes.add("limites de força cortante positivos");
        }
        if (serieBm.stream().anyMatch(valor -> valor == null || valor <= 0.0)) {
            ausentes.add("limites de momento fletor positivos");
        }
    }

    private void validarCoerenciaDistribuicao(NavioGranel navio, List<Double> pesoLeveSecoes,
            List<Double> empuxoSecoes, List<ViolacaoEstivaDto> violacoes) {
        double somaPesoLeve = pesoLeveSecoes.stream().mapToDouble(Double::doubleValue).sum();
        double tolerancia = Math.max(1.0, navio.getPesoLeveToneladas() * TOLERANCIA_DISTRIBUICAO);
        if (Math.abs(somaPesoLeve - navio.getPesoLeveToneladas()) > tolerancia) {
            violacoes.add(new ViolacaoEstivaDto(
                    "DISTRIBUICAO_PESO_LEVE_INCONSISTENTE",
                    String.format(Locale.ROOT,
                            "Soma da distribuição de peso leve %.1f t diverge do peso leve informado %.1f t",
                            somaPesoLeve, navio.getPesoLeveToneladas()),
                    null,
                    "PERIGO"));
        }
        if (empuxoSecoes.stream().mapToDouble(Double::doubleValue).sum() <= 0.0) {
            violacoes.add(new ViolacaoEstivaDto(
                    "CURVA_EMPUXO_INVALIDA",
                    "A curva de empuxo versionada não possui deslocamento positivo",
                    null,
                    "PERIGO"));
        }
    }

    private ResultadoLongitudinal calcularEsforcosLongitudinais(List<PosicaoBobina> posicoesCarga,
            NavioGranel navio, List<Double> posicoes, List<Double> pesoLeve, List<Double> empuxo,
            List<Double> limitesSf, List<Double> limitesBm, double pesoTotalToneladas,
            List<ViolacaoEstivaDto> violacoes) {
        double[] pesos = pesoLeve.stream().mapToDouble(Double::doubleValue).toArray();
        for (PosicaoBobina posicao : posicoesCarga) {
            adicionarPesoNaSecaoMaisProxima(
                    pesos,
                    posicoes,
                    calcularPosicaoLongitudinal(posicao),
                    posicao.getBobina().getPesoKg() / 1000.0);
        }
        double pesoLastro = valorOuZero(navio.getPesoLastroToneladas());
        if (pesoLastro > 0.0) {
            adicionarPesoNaSecaoMaisProxima(pesos, posicoes, navio.getLcgLastro(), pesoLastro);
        }

        double somaEmpuxo = empuxo.stream().mapToDouble(Double::doubleValue).sum();
        double fatorEmpuxo = pesoTotalToneladas / somaEmpuxo;
        double sf = 0.0;
        double bm = 0.0;
        double sfMax = 0.0;
        double bmMax = 0.0;
        double bmAssinadoNoMaximo = 0.0;

        for (int i = 0; i < posicoes.size(); i++) {
            double cargaLiquidaKn = (pesos[i] - empuxo.get(i) * fatorEmpuxo) * ACELERACAO_GRAVIDADE;
            sf += cargaLiquidaKn;
            if (i > 0) {
                double delta = posicoes.get(i) - posicoes.get(i - 1);
                bm += sf * delta;
            }
            sfMax = Math.max(sfMax, Math.abs(sf));
            if (Math.abs(bm) > bmMax) {
                bmMax = Math.abs(bm);
                bmAssinadoNoMaximo = bm;
            }
            verificarLimiteSecao("FORCA_CISALHAMENTO", sf, limitesSf.get(i), posicoes.get(i), violacoes);
            verificarLimiteSecao("MOMENTO_FLETOR", bm, limitesBm.get(i), posicoes.get(i), violacoes);
        }
        return new ResultadoLongitudinal(sfMax, bmMax, bmAssinadoNoMaximo);
    }

    private void verificarLimiteSecao(String tipo, double valor, double limite, double posicao,
            List<ViolacaoEstivaDto> violacoes) {
        double utilizacao = Math.abs(valor) / limite;
        if (utilizacao > 1.0) {
            violacoes.add(new ViolacaoEstivaDto(
                    tipo + "_EXCEDIDO",
                    String.format(Locale.ROOT,
                            "%s %.1f na seção %.2f m excede o limite versionado %.1f",
                            tipo, Math.abs(valor), posicao, limite),
                    null,
                    "PERIGO"));
        } else if (utilizacao > 0.8) {
            violacoes.add(new ViolacaoEstivaDto(
                    tipo + "_PROXIMO_LIMITE",
                    String.format(Locale.ROOT,
                            "%s %.1f na seção %.2f m atingiu %.1f%% do limite versionado",
                            tipo, Math.abs(valor), posicao, utilizacao * 100.0),
                    null,
                    "AVISO"));
        }
    }

    private void verificarLimiteGlobal(String tipo, double valor, double limite,
            List<ViolacaoEstivaDto> violacoes) {
        if (valor > limite) {
            violacoes.add(new ViolacaoEstivaDto(
                    tipo + "_GLOBAL_EXCEDIDO",
                    String.format(Locale.ROOT,
                            "%s máximo %.1f excede o limite global versionado %.1f", tipo, valor, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void adicionarPesoNaSecaoMaisProxima(double[] pesos, List<Double> posicoes,
            double posicaoCarga, double pesoToneladas) {
        int indice = 0;
        double menorDistancia = Double.MAX_VALUE;
        for (int i = 0; i < posicoes.size(); i++) {
            double distancia = Math.abs(posicoes.get(i) - posicaoCarga);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                indice = i;
            }
        }
        pesos[indice] += pesoToneladas;
    }

    private void verificarCalado(double calado, double limite, List<ViolacaoEstivaDto> violacoes) {
        if (calado > limite) {
            violacoes.add(new ViolacaoEstivaDto(
                    "CALADO_EXCEDIDO",
                    String.format(Locale.ROOT,
                            "Calado médio %.2f m excede o limite da condição %.2f m", calado, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarTrim(double trim, double limite, List<ViolacaoEstivaDto> violacoes) {
        if (Math.abs(trim) > limite) {
            violacoes.add(new ViolacaoEstivaDto(
                    "TRIM_EXCEDIDO",
                    String.format(Locale.ROOT,
                            "Trim %.2f m excede o limite versionado ±%.2f m", trim, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarBanda(double banda, double limite, List<ViolacaoEstivaDto> violacoes) {
        if (Math.abs(banda) > limite) {
            violacoes.add(new ViolacaoEstivaDto(
                    "BANDA_EXCEDIDA",
                    String.format(Locale.ROOT,
                            "Banda %.2f° excede o limite versionado ±%.2f°", banda, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarGm(double gm, double limite, List<ViolacaoEstivaDto> violacoes) {
        if (gm < limite) {
            violacoes.add(new ViolacaoEstivaDto(
                    "GM_INSUFICIENTE",
                    String.format(Locale.ROOT,
                            "GM %.3f m é inferior ao mínimo versionado %.3f m", gm, limite),
                    null,
                    "PERIGO"));
        }
    }

    private double calcularPesoCargaToneladas(List<PosicaoBobina> posicoes) {
        return posicoes.stream()
                .filter(posicao -> posicao.getBobina() != null && posicao.getBobina().getPesoKg() != null)
                .mapToDouble(posicao -> posicao.getBobina().getPesoKg() / 1000.0)
                .sum();
    }

    private double calcularPosicaoLongitudinal(PosicaoBobina posicao) {
        PoraoNavio porao = posicao.getPorao();
        return porao.getPosLongInicio() + posicao.getPosicaoX();
    }

    private double calcularPosicaoVertical(PosicaoBobina posicao) {
        double diametroMetros = posicao.getBobina().getDiametroExternoMm() / 1000.0;
        double dunnageMetros = posicao.getEspessuraDunnageMm() / 1000.0;
        return dunnageMetros + diametroMetros / 2.0 + (posicao.getCamada() - 1) * diametroMetros;
    }

    private List<Double> parseSerie(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(valor.split(";"))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .map(Double::valueOf)
                    .toList();
        } catch (NumberFormatException excecao) {
            return List.of();
        }
    }

    private void exigirTexto(String valor, String campo, List<String> ausentes) {
        if (valor == null || valor.isBlank()) {
            ausentes.add(campo);
        }
    }

    private void exigirNumero(Double valor, String campo, List<String> ausentes) {
        if (valor == null || !Double.isFinite(valor)) {
            ausentes.add(campo);
        }
    }

    private void exigirPositivo(Double valor, String campo, List<String> ausentes) {
        if (valor == null || !Double.isFinite(valor) || valor <= 0.0) {
            ausentes.add(campo);
        }
    }

    private String identificarPosicao(PosicaoBobina posicao) {
        return posicao.getId() != null ? posicao.getId().toString() : "não persistida";
    }

    private double valorOuZero(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private String valorOuAusente(String valor) {
        return valor != null && !valor.isBlank() ? valor : "AUSENTE";
    }

    private double arredondar(double valor, int casas) {
        double fator = Math.pow(10.0, casas);
        return Math.round(valor * fator) / fator;
    }

    private String criarMemoriaCalculo(NavioGranel navio, EstabilidadeEstrutural dto) {
        return String.format(Locale.ROOT,
                "OPERACIONAL;hidro=%s;estrutural=%s;pesoTotal=%.1ft;calado=%.2fm;trim=%.2fm;"
                        + "banda=%.2fgraus;gm=%.3fm;sfMax=%.1fkN;bmMax=%.1fkNm",
                navio.getVersaoDadosHidrostaticos(),
                navio.getVersaoDadosEstruturais(),
                dto.getPesoTotalToneladas(),
                dto.getCaladoSaidaMetros(),
                dto.getTrimMetros(),
                dto.getListGraus(),
                dto.getGmMetros(),
                dto.getSfMaxKn(),
                dto.getBmMaxKnm());
    }

    private static final class ResultadoLongitudinal {
        private final double sfMaxKn;
        private final double bmMaxKnm;
        private final double bmAssinadoNoMaximo;

        private ResultadoLongitudinal(double sfMaxKn, double bmMaxKnm, double bmAssinadoNoMaximo) {
            this.sfMaxKn = sfMaxKn;
            this.bmMaxKnm = bmMaxKnm;
            this.bmAssinadoNoMaximo = bmAssinadoNoMaximo;
        }
    }
}
