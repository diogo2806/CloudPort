package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ViolacaoHardConstraintDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class EstabilidadeNavioServico {

    private static final double ACELERACAO_GRAVIDADE = 9.80665;
    private static final double TOLERANCIA_DISTRIBUICAO = 0.02;

    public EstabilidadeDto calcular(EstivagemPlan plan) {
        List<SlotNavio> slots = plan.getSlots() != null ? plan.getSlots() : List.of();
        List<SlotNavio> ocupados = slots.stream()
                .filter(slot -> slot.getCodigoContainer() != null)
                .toList();

        EstabilidadeDto dto = EstabilidadeDto.vazia();
        dto.setVersaoDadosHidrostaticos(plan.getVersaoDadosHidrostaticos());
        dto.setVersaoDadosEstruturais(plan.getVersaoDadosEstruturais());

        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();
        verificarSobrePeso(slots, violacoes);
        verificarSegregacaoImo(slots, violacoes);
        verificarReefer(slots, violacoes);
        verificarOog(slots, violacoes);

        List<String> dadosAusentes = validarDadosOperacionais(plan, ocupados);
        if (!dadosAusentes.isEmpty()) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "DADOS_ESTABILIDADE_INCOMPLETOS",
                    "Cálculo identificado como simulação não operacional. Dados obrigatórios ausentes ou inválidos: "
                            + String.join(", ", dadosAusentes),
                    null,
                    "PERIGO"));
            dto.setPesoTotalToneladas(arredondar(calcularPesoCargaToneladas(ocupados), 1));
            dto.setOperacional(false);
            dto.setAprovado(false);
            dto.setMemoriaCalculo("SIMULACAO_NAO_OPERACIONAL; aprovação bloqueada; hidro="
                    + valorOuAusente(plan.getVersaoDadosHidrostaticos()) + "; estrutural="
                    + valorOuAusente(plan.getVersaoDadosEstruturais()));
            dto.setViolacoes(violacoes);
            return dto;
        }

        List<Double> posicoesSecoes = parseSerie(plan.getPosicoesSecoes());
        List<Double> pesoLeveSecoes = parseSerie(plan.getPesoLeveSecoes());
        List<Double> empuxoSecoes = parseSerie(plan.getEmpuxoSecoes());
        List<Double> limitesSfSecoes = parseSerie(plan.getLimitesSfSecoes());
        List<Double> limitesBmSecoes = parseSerie(plan.getLimitesBmSecoes());

        double pesoCargaToneladas = calcularPesoCargaToneladas(ocupados);
        double pesoLastroToneladas = valorOuZero(plan.getPesoLastroToneladas());
        double pesoTotalToneladas = plan.getPesoLeveToneladas() + pesoLastroToneladas + pesoCargaToneladas;

        double momentoLongitudinal = plan.getPesoLeveToneladas() * plan.getLcgPesoLeve();
        double momentoTransversal = plan.getPesoLeveToneladas() * plan.getTcgPesoLeve();
        double momentoVertical = plan.getPesoLeveToneladas() * plan.getVcgPesoLeve();

        if (pesoLastroToneladas > 0.0) {
            momentoLongitudinal += pesoLastroToneladas * plan.getLcgLastro();
            momentoTransversal += pesoLastroToneladas * plan.getTcgLastro();
            momentoVertical += pesoLastroToneladas * plan.getVcgLastro();
        }

        for (SlotNavio slot : ocupados) {
            double pesoToneladas = slot.getPesoKg() / 1000.0;
            momentoLongitudinal += pesoToneladas * slot.getPosLongitudinalMetros();
            momentoTransversal += pesoToneladas * slot.getPosTransversalMetros();
            momentoVertical += pesoToneladas * slot.getPosVerticalMetros();
        }

        double lcg = momentoLongitudinal / pesoTotalToneladas;
        double tcg = momentoTransversal / pesoTotalToneladas;
        double vcg = momentoVertical / pesoTotalToneladas;
        double gm = plan.getKm() - vcg;
        double calado = plan.getCalado()
                + (pesoTotalToneladas - plan.getDeslocamento()) / (plan.getTpc() * 100.0);
        double trim = ((lcg - plan.getLcb()) * pesoTotalToneladas) / (plan.getMct1cm() * 100.0);
        double banda = gm != 0.0 ? Math.toDegrees(Math.atan(tcg / gm)) : 90.0;

        validarCoerenciaDistribuicao(plan, pesoLeveSecoes, empuxoSecoes, violacoes);
        ResultadoLongitudinal longitudinal = calcularEsforcosLongitudinais(
                ocupados, plan, posicoesSecoes, pesoLeveSecoes, empuxoSecoes,
                limitesSfSecoes, limitesBmSecoes, pesoTotalToneladas, violacoes);

        verificarCalado(calado, plan.getCaladoMaximo(), violacoes);
        verificarTrim(trim, plan.getTrimMaximo(), violacoes);
        verificarList(banda, plan.getBandaMaxima(), violacoes);
        verificarGm(gm, plan.getGmMinimo(), violacoes);

        boolean aprovado = violacoes.stream()
                .noneMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));

        dto.setTrimMetros(arredondar(trim, 2));
        dto.setListGraus(arredondar(banda, 2));
        dto.setCaladoMedioMetros(arredondar(calado, 2));
        dto.setGmMetros(arredondar(gm, 3));
        dto.setLcgMetros(arredondar(lcg, 2));
        dto.setTcgMetros(arredondar(tcg, 2));
        dto.setVcgMetros(arredondar(vcg, 2));
        dto.setSfMaxKn(arredondar(longitudinal.sfMaxKn, 1));
        dto.setBmMaxKnm(arredondar(longitudinal.bmMaxKnm, 1));
        dto.setPesoTotalToneladas(arredondar(pesoTotalToneladas, 1));
        dto.setOperacional(true);
        dto.setAprovado(aprovado);
        dto.setMemoriaCalculo(criarMemoriaCalculo(plan, dto));
        dto.setViolacoes(violacoes);
        return dto;
    }

    public List<ViolacaoHardConstraintDto> verificarSlot(EstivagemPlan plan, SlotNavio slot,
            String codigoContainer, Double pesoKg, String classeImo, boolean reefer) {
        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();
        if (slot.getMaxPesoKg() != null && pesoKg != null && pesoKg > slot.getMaxPesoKg()) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "SOBREPESO_SLOT",
                    "Peso " + pesoKg + " kg excede o limite do slot " + slot.getMaxPesoKg() + " kg",
                    slot.getId(),
                    "PERIGO"));
        }
        if (reefer && !ehSlotReefer(slot.getTipoSlot())) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "REEFER_SLOT_INVALIDO",
                    "Container reefer não pode ser alocado em slot tipo " + slot.getTipoSlot(),
                    slot.getId(),
                    "PERIGO"));
        }
        if (classeImo != null && !classeImo.isBlank() && !ehSlotPerigoso(slot.getTipoSlot())) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "SEGREGACAO_IMO_VIOLADA",
                    "Carga IMO classe " + classeImo + " exige slot perigoso dedicado",
                    slot.getId(),
                    "PERIGO"));
        }
        return violacoes;
    }

    private List<String> validarDadosOperacionais(EstivagemPlan plan, List<SlotNavio> ocupados) {
        List<String> ausentes = new ArrayList<>();
        exigirTexto(plan.getVersaoDadosHidrostaticos(), "versaoDadosHidrostaticos", ausentes);
        exigirTexto(plan.getVersaoDadosEstruturais(), "versaoDadosEstruturais", ausentes);
        exigirPositivo(plan.getComprimentoLpp(), "comprimentoLpp", ausentes);
        exigirPositivo(plan.getBoca(), "boca", ausentes);
        exigirPositivo(plan.getCalado(), "caladoReferencia", ausentes);
        exigirPositivo(plan.getDeslocamento(), "deslocamentoReferencia", ausentes);
        exigirPositivo(plan.getTpc(), "tpc", ausentes);
        exigirNumero(plan.getLcb(), "lcb", ausentes);
        exigirPositivo(plan.getKm(), "km", ausentes);
        exigirPositivo(plan.getMct1cm(), "mct1cm", ausentes);
        exigirPositivo(plan.getCaladoMaximo(), "caladoMaximo", ausentes);
        exigirPositivo(plan.getTrimMaximo(), "trimMaximo", ausentes);
        exigirPositivo(plan.getBandaMaxima(), "bandaMaxima", ausentes);
        exigirPositivo(plan.getGmMinimo(), "gmMinimo", ausentes);
        exigirPositivo(plan.getPesoLeveToneladas(), "pesoLeveToneladas", ausentes);
        exigirNumero(plan.getLcgPesoLeve(), "lcgPesoLeve", ausentes);
        exigirNumero(plan.getTcgPesoLeve(), "tcgPesoLeve", ausentes);
        exigirNumero(plan.getVcgPesoLeve(), "vcgPesoLeve", ausentes);
        if (valorOuZero(plan.getPesoLastroToneladas()) > 0.0) {
            exigirNumero(plan.getLcgLastro(), "lcgLastro", ausentes);
            exigirNumero(plan.getTcgLastro(), "tcgLastro", ausentes);
            exigirNumero(plan.getVcgLastro(), "vcgLastro", ausentes);
        }
        validarSeries(plan.getPosicoesSecoes(), plan.getPesoLeveSecoes(), plan.getEmpuxoSecoes(),
                plan.getLimitesSfSecoes(), plan.getLimitesBmSecoes(), ausentes);
        if (ocupados.isEmpty()) {
            ausentes.add("cargaPlanejada");
        }
        for (SlotNavio slot : ocupados) {
            if (slot.getPesoKg() == null || slot.getPesoKg() <= 0.0) {
                ausentes.add("pesoKg do slot " + identificarSlot(slot));
            }
            if (slot.getPosLongitudinalMetros() == null
                    || slot.getPosTransversalMetros() == null
                    || slot.getPosVerticalMetros() == null) {
                ausentes.add("coordenadas físicas do slot " + identificarSlot(slot));
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
        if (quantidade < 2 || seriePeso.size() != quantidade || serieEmpuxo.size() != quantidade
                || serieSf.size() != quantidade || serieBm.size() != quantidade) {
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

    private void validarCoerenciaDistribuicao(EstivagemPlan plan, List<Double> pesoLeveSecoes,
            List<Double> empuxoSecoes, List<ViolacaoHardConstraintDto> violacoes) {
        double somaPesoLeve = pesoLeveSecoes.stream().mapToDouble(Double::doubleValue).sum();
        double tolerancia = Math.max(1.0, plan.getPesoLeveToneladas() * TOLERANCIA_DISTRIBUICAO);
        if (Math.abs(somaPesoLeve - plan.getPesoLeveToneladas()) > tolerancia) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "DISTRIBUICAO_PESO_LEVE_INCONSISTENTE",
                    String.format(Locale.ROOT,
                            "Soma da distribuição de peso leve %.1f t diverge do peso leve informado %.1f t",
                            somaPesoLeve, plan.getPesoLeveToneladas()),
                    null,
                    "PERIGO"));
        }
        if (empuxoSecoes.stream().mapToDouble(Double::doubleValue).sum() <= 0.0) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "CURVA_EMPUXO_INVALIDA",
                    "A curva de empuxo versionada não possui deslocamento positivo",
                    null,
                    "PERIGO"));
        }
    }

    private ResultadoLongitudinal calcularEsforcosLongitudinais(List<SlotNavio> ocupados,
            EstivagemPlan plan, List<Double> posicoes, List<Double> pesoLeve, List<Double> empuxo,
            List<Double> limitesSf, List<Double> limitesBm, double pesoTotalToneladas,
            List<ViolacaoHardConstraintDto> violacoes) {
        double[] pesos = pesoLeve.stream().mapToDouble(Double::doubleValue).toArray();
        for (SlotNavio slot : ocupados) {
            adicionarPesoNaSecaoMaisProxima(
                    pesos, posicoes, slot.getPosLongitudinalMetros(), slot.getPesoKg() / 1000.0);
        }
        double pesoLastro = valorOuZero(plan.getPesoLastroToneladas());
        if (pesoLastro > 0.0) {
            adicionarPesoNaSecaoMaisProxima(pesos, posicoes, plan.getLcgLastro(), pesoLastro);
        }
        double somaEmpuxo = empuxo.stream().mapToDouble(Double::doubleValue).sum();
        double fatorEmpuxo = pesoTotalToneladas / somaEmpuxo;
        double sf = 0.0;
        double bm = 0.0;
        double sfMax = 0.0;
        double bmMax = 0.0;
        for (int i = 0; i < posicoes.size(); i++) {
            double cargaLiquidaKn = (pesos[i] - empuxo.get(i) * fatorEmpuxo) * ACELERACAO_GRAVIDADE;
            sf += cargaLiquidaKn;
            if (i > 0) {
                bm += sf * (posicoes.get(i) - posicoes.get(i - 1));
            }
            sfMax = Math.max(sfMax, Math.abs(sf));
            bmMax = Math.max(bmMax, Math.abs(bm));
            verificarLimiteSecao("FORCA_CISALHAMENTO", sf, limitesSf.get(i), posicoes.get(i), violacoes);
            verificarLimiteSecao("MOMENTO_FLETOR", bm, limitesBm.get(i), posicoes.get(i), violacoes);
        }
        return new ResultadoLongitudinal(sfMax, bmMax);
    }

    private void verificarLimiteSecao(String tipo, double valor, double limite, double posicao,
            List<ViolacaoHardConstraintDto> violacoes) {
        double utilizacao = Math.abs(valor) / limite;
        if (utilizacao > 1.0) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    tipo + "_EXCEDIDO",
                    String.format(Locale.ROOT, "%s %.1f na seção %.2f m excede o limite versionado %.1f",
                            tipo, Math.abs(valor), posicao, limite),
                    null,
                    "PERIGO"));
        } else if (utilizacao > 0.8) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    tipo + "_PROXIMO_LIMITE",
                    String.format(Locale.ROOT, "%s %.1f na seção %.2f m atingiu %.1f%% do limite versionado",
                            tipo, Math.abs(valor), posicao, utilizacao * 100.0),
                    null,
                    "AVISO"));
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

    private void verificarCalado(double calado, double limite, List<ViolacaoHardConstraintDto> violacoes) {
        if (calado > limite) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "CALADO_EXCEDIDO",
                    String.format(Locale.ROOT, "Calado médio %.2f m excede o limite da condição %.2f m",
                            calado, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarTrim(double trim, double limite, List<ViolacaoHardConstraintDto> violacoes) {
        if (Math.abs(trim) > limite) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "TRIM_EXCEDIDO",
                    String.format(Locale.ROOT, "Trim %.2f m excede o limite versionado ±%.2f m", trim, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarList(double list, double limite, List<ViolacaoHardConstraintDto> violacoes) {
        if (Math.abs(list) > limite) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "LIST_EXCEDIDO",
                    String.format(Locale.ROOT, "Banda %.2f° excede o limite versionado ±%.2f°", list, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarGm(double gm, double limite, List<ViolacaoHardConstraintDto> violacoes) {
        if (gm < limite) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "GM_INSUFICIENTE",
                    String.format(Locale.ROOT, "GM %.3f m é inferior ao mínimo versionado %.3f m", gm, limite),
                    null,
                    "PERIGO"));
        }
    }

    private void verificarSobrePeso(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio slot : slots) {
            if (slot.getCodigoContainer() != null && slot.getMaxPesoKg() != null
                    && slot.getPesoKg() != null && slot.getPesoKg() > slot.getMaxPesoKg()) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "SOBREPESO_SLOT",
                        "Slot " + identificarSlot(slot) + ": peso " + slot.getPesoKg()
                                + " kg > máximo " + slot.getMaxPesoKg() + " kg",
                        slot.getId(),
                        "PERIGO"));
            }
        }
    }

    private void verificarSegregacaoImo(List<SlotNavio> slots,
            List<ViolacaoHardConstraintDto> violacoes) {
        List<SlotNavio> perigosos = slots.stream()
                .filter(slot -> slot.getCodigoContainer() != null)
                .filter(slot -> slot.isPerigoso()
                        || (slot.getClasseImo() != null && !slot.getClasseImo().isBlank()))
                .toList();
        for (SlotNavio slot : perigosos) {
            if (!ehSlotPerigoso(slot.getTipoSlot())) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "SEGREGACAO_IMO_VIOLADA",
                        "Container IMO " + slot.getCodigoContainer() + " (classe " + slot.getClasseImo()
                                + ") fora de slot perigoso dedicado",
                        slot.getId(),
                        "PERIGO"));
            }
        }
        for (int primeiro = 0; primeiro < perigosos.size(); primeiro++) {
            for (int segundo = primeiro + 1; segundo < perigosos.size(); segundo++) {
                SlotNavio slotA = perigosos.get(primeiro);
                SlotNavio slotB = perigosos.get(segundo);
                if (adjacentes(slotA, slotB) && !mesmaClasseOuGrupo(slotA, slotB)) {
                    violacoes.add(new ViolacaoHardConstraintDto(
                            "SEGREGACAO_IMO_VIOLADA",
                            "Cargas perigosas incompatíveis estão em posições adjacentes: "
                                    + slotA.getCodigoContainer() + " e " + slotB.getCodigoContainer(),
                            slotB.getId(),
                            "PERIGO"));
                }
            }
        }
    }

    private void verificarReefer(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio slot : slots) {
            if (slot.isReefer() && slot.getCodigoContainer() != null && !ehSlotReefer(slot.getTipoSlot())) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "REEFER_SLOT_INVALIDO",
                        "Container reefer " + slot.getCodigoContainer() + " em slot não reefer",
                        slot.getId(),
                        "PERIGO"));
            }
        }
    }

    private void verificarOog(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio slot : slots) {
            if (slot.isOog() && slot.getCodigoContainer() != null && slot.getTipoSlot() != TipoSlotNavio.OOG) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "OOG_SLOT_INVALIDO",
                        "Container OOG " + slot.getCodigoContainer() + " em slot sem reserva dimensional",
                        slot.getId(),
                        "PERIGO"));
            }
        }
    }

    private boolean ehSlotReefer(TipoSlotNavio tipoSlot) {
        return tipoSlot == TipoSlotNavio.REEFER || tipoSlot == TipoSlotNavio.REEFER_PERIGOSO;
    }

    private boolean ehSlotPerigoso(TipoSlotNavio tipoSlot) {
        return tipoSlot == TipoSlotNavio.PERIGOSO || tipoSlot == TipoSlotNavio.REEFER_PERIGOSO;
    }

    private boolean adjacentes(SlotNavio primeiro, SlotNavio segundo) {
        return primeiro.getBay() == segundo.getBay()
                && Math.abs(primeiro.getRowBay() - segundo.getRowBay()) <= 1
                && Math.abs(primeiro.getTier() - segundo.getTier()) <= 1;
    }

    private boolean mesmaClasseOuGrupo(SlotNavio primeiro, SlotNavio segundo) {
        if (iguaisNaoVazios(primeiro.getClasseImo(), segundo.getClasseImo())) {
            return true;
        }
        return iguaisNaoVazios(primeiro.getGrupoSegregacao(), segundo.getGrupoSegregacao());
    }

    private boolean iguaisNaoVazios(String primeiro, String segundo) {
        return primeiro != null && !primeiro.isBlank() && segundo != null && !segundo.isBlank()
                && primeiro.equalsIgnoreCase(segundo);
    }

    private double calcularPesoCargaToneladas(List<SlotNavio> ocupados) {
        return ocupados.stream()
                .mapToDouble(slot -> slot.getPesoKg() != null ? slot.getPesoKg() / 1000.0 : 0.0)
                .sum();
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

    private String identificarSlot(SlotNavio slot) {
        return "bay=" + slot.getBay() + ",row=" + slot.getRowBay() + ",tier=" + slot.getTier();
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

    private String criarMemoriaCalculo(EstivagemPlan plan, EstabilidadeDto dto) {
        return String.format(Locale.ROOT,
                "OPERACIONAL;hidro=%s;estrutural=%s;pesoTotal=%.1ft;calado=%.2fm;trim=%.2fm;"
                        + "banda=%.2fgraus;gm=%.3fm;sfMax=%.1fkN;bmMax=%.1fkNm",
                plan.getVersaoDadosHidrostaticos(), plan.getVersaoDadosEstruturais(),
                dto.getPesoTotalToneladas(), dto.getCaladoMedioMetros(), dto.getTrimMetros(),
                dto.getListGraus(), dto.getGmMetros(), dto.getSfMaxKn(), dto.getBmMaxKnm());
    }

    private static final class ResultadoLongitudinal {
        private final double sfMaxKn;
        private final double bmMaxKnm;

        private ResultadoLongitudinal(double sfMaxKn, double bmMaxKnm) {
            this.sfMaxKn = sfMaxKn;
            this.bmMaxKnm = bmMaxKnm;
        }
    }
}
