package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.SchedulerAssignmentDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerContainerDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerEquipmentDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPositionCandidateDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RealYardReplanningOptimizerService {

    private static final double PESO_DISTANCIA = 1.0;
    private static final double PESO_OCUPACAO = 18.0;
    private static final double PESO_REHANDLE = 7.0;
    private static final double PESO_DESTINO = 12.0;
    private static final double PESO_EQUIPAMENTO = 1.0;
    private static final double PESO_SEQUENCIA = 0.25;
    private static final double PESO_DWELL = 0.40;

    public ResultadoOtimizacaoReal otimizar(SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        List<SchedulerContainerDto> itens = Stream.concat(
                        safe(requisicao.getContainersImportacao()).stream(),
                        safe(requisicao.getContainersExportacao()).stream())
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getCodigoContainer()))
                .sorted(Comparator
                        .comparing(SchedulerContainerDto::getSequenciaOperacional,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(item -> normalizar(item.getCodigoContainer())))
                .toList();
        List<SchedulerPositionCandidateDto> candidatos = safe(requisicao.getPosicoesCandidatas()).stream()
                .filter(Objects::nonNull)
                .filter(posicao -> posicao.getLinha() != null
                        && posicao.getColuna() != null
                        && StringUtils.hasText(posicao.getCamada()))
                .sorted(Comparator
                        .comparing(SchedulerPositionCandidateDto::getLinha)
                        .thenComparing(SchedulerPositionCandidateDto::getColuna)
                        .thenComparing(posicao -> normalizar(posicao.getCamada()))
                        .thenComparing(SchedulerPositionCandidateDto::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("O replanejamento real exige ao menos uma carga elegivel.");
        }
        if (candidatos.isEmpty()) {
            throw new IllegalArgumentException("O replanejamento real exige o mapa completo de posicoes candidatas.");
        }

        List<SchedulerEquipmentDto> equipamentos = equipamentosDisponiveis(requisicao);
        if (equipamentos.isEmpty()) {
            throw new IllegalArgumentException("O replanejamento real exige equipamento operacional disponivel.");
        }

        Set<String> posicoesUtilizadas = new LinkedHashSet<>();
        List<SchedulerAssignmentDto> atribuicoes = new ArrayList<>();
        List<String> justificativasGerais = new ArrayList<>();
        Map<String, Double> memoria = memoriaVazia();
        int sequencia = 0;
        int distanciaOriginal = 0;
        int distanciaOtimizada = 0;
        int rehandlesEstimados = 0;
        double pontuacaoTotal = 0.0;

        for (SchedulerContainerDto item : itens) {
            int custoOriginalItem = custoOriginal(item);
            Avaliacao melhor = candidatos.stream()
                    .filter(candidato -> !posicoesUtilizadas.contains(chave(candidato)))
                    .filter(candidato -> compativel(item, candidato))
                    .map(candidato -> avaliar(item, candidato, equipamentos, requisicao))
                    .min(Comparator.comparingDouble(Avaliacao::score)
                            .thenComparing(avaliacao -> chave(avaliacao.posicao())))
                    .orElse(null);
            if (melhor == null) {
                justificativasGerais.add("Nenhuma posicao real compativel foi encontrada para "
                        + item.getCodigoContainer() + ".");
                continue;
            }

            sequencia++;
            posicoesUtilizadas.add(chave(melhor.posicao()));
            SchedulerAssignmentDto atribuicao = criarAtribuicao(item, melhor, sequencia);
            atribuicoes.add(atribuicao);
            item.setLinha(melhor.posicao().getLinha());
            item.setColuna(melhor.posicao().getColuna());
            item.setCamadaAtual(melhor.posicao().getCamada());

            distanciaOriginal += custoOriginalItem;
            distanciaOtimizada += melhor.distanciaMovimento() + melhor.distanciaBerco();
            rehandlesEstimados += melhor.rehandles();
            pontuacaoTotal += melhor.score();
            acumular(memoria, "distancia", melhor.distanciaPonderada());
            acumular(memoria, "ocupacao", melhor.penalidadeOcupacao());
            acumular(memoria, "rehandles", melhor.penalidadeRehandle());
            acumular(memoria, "destino", melhor.penalidadeDestino());
            acumular(memoria, "equipamento", melhor.penalidadeEquipamento());
            acumular(memoria, "sequencia", melhor.penalidadeSequencia());
            acumular(memoria, "dwellTime", melhor.bonusDwell());
        }

        if (atribuicoes.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma carga possui posicao real elegivel para replanejamento.");
        }
        memoria.put("pontuacaoTotal", arredondar(pontuacaoTotal));
        memoria.put("itensAtribuidos", (double) atribuicoes.size());
        memoria.put("itensSemAtribuicao", (double) Math.max(0, itens.size() - atribuicoes.size()));
        memoria.put("janelaOperacionalHoras", janelaOperacionalHoras(requisicao));
        memoria.put("cutoffRestanteHoras", cutoffRestanteHoras(requisicao));

        String assinatura = assinatura(requisicao, atribuicoes);
        justificativasGerais.add("Plano reproduzivel pela assinatura " + assinatura + ".");
        justificativasGerais.add("Todas as posicoes propostas permanecem sujeitas a revalidacao transacional no momento da aplicacao.");

        return new ResultadoOtimizacaoReal(
                List.copyOf(atribuicoes),
                Map.copyOf(memoria),
                List.copyOf(justificativasGerais),
                assinatura,
                rehandlesEstimados,
                distanciaOriginal,
                distanciaOtimizada,
                arredondar(pontuacaoTotal));
    }

    private Avaliacao avaliar(
            SchedulerContainerDto item,
            SchedulerPositionCandidateDto posicao,
            List<SchedulerEquipmentDto> equipamentos,
            SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        int linhaAtual = valor(item.getLinha());
        int colunaAtual = valor(item.getColuna());
        int distanciaMovimento = Math.abs(linhaAtual - posicao.getLinha())
                + Math.abs(colunaAtual - posicao.getColuna());
        int distanciaBerco = posicao.getDistanciaBerco() == null
                ? Math.abs(posicao.getLinha()) + Math.abs(posicao.getColuna())
                : Math.max(0, posicao.getDistanciaBerco());
        double urgencia = fatorUrgencia(requisicao);
        double distanciaPonderada = (distanciaMovimento + distanciaBerco)
                * peso(requisicao, "DISTANCIA", PESO_DISTANCIA)
                * urgencia;
        double ocupacao = taxaOcupacao(posicao)
                * peso(requisicao, "OCUPACAO", PESO_OCUPACAO);
        int rehandles = estimarRehandles(posicao);
        double penalidadeRehandle = rehandles
                * peso(requisicao, "REHANDLE", PESO_REHANDLE);
        double penalidadeDestino = destinoCompativel(item, posicao)
                ? 0.0
                : peso(requisicao, "DESTINO", PESO_DESTINO);
        SchedulerEquipmentDto equipamento = selecionarEquipamento(posicao, equipamentos);
        double penalidadeEquipamento = custoEquipamento(posicao, equipamento)
                * peso(requisicao, "EQUIPAMENTO", PESO_EQUIPAMENTO);
        double penalidadeSequencia = valor(item.getSequenciaOperacional())
                * peso(requisicao, "SEQUENCIA", PESO_SEQUENCIA);
        double bonusDwell = -Math.min(20.0, valor(item.getDwellTimeHoras()) / 24.0)
                * peso(requisicao, "DWELL", PESO_DWELL);
        double score = distanciaPonderada + ocupacao + penalidadeRehandle
                + penalidadeDestino + penalidadeEquipamento + penalidadeSequencia + bonusDwell;
        return new Avaliacao(
                posicao,
                equipamento,
                score,
                distanciaMovimento,
                distanciaBerco,
                distanciaPonderada,
                ocupacao,
                rehandles,
                penalidadeRehandle,
                penalidadeDestino,
                penalidadeEquipamento,
                penalidadeSequencia,
                bonusDwell);
    }

    private SchedulerAssignmentDto criarAtribuicao(
            SchedulerContainerDto item,
            Avaliacao avaliacao,
            int sequencia) {
        SchedulerAssignmentDto dto = new SchedulerAssignmentDto();
        dto.setCodigoContainer(item.getCodigoContainer());
        dto.setMovimento(item.getMovimento());
        dto.setLinhaOriginal(item.getLinha());
        dto.setColunaOriginal(item.getColuna());
        dto.setCamadaOriginal(item.getCamadaAtual());
        dto.setLinhaProposta(avaliacao.posicao().getLinha());
        dto.setColunaProposta(avaliacao.posicao().getColuna());
        dto.setCamadaProposta(avaliacao.posicao().getCamada());
        dto.setBlocoProposto(avaliacao.posicao().getBloco());
        dto.setEquipamentoId(avaliacao.equipamento().getEquipamentoId());
        dto.setSequenciaPlano(sequencia);
        dto.setScoreTotal(arredondar(avaliacao.score()));
        dto.setDistancia(avaliacao.distanciaMovimento() + avaliacao.distanciaBerco());
        dto.setRehandlesEstimados(avaliacao.rehandles());
        dto.setPenalidadeOcupacao(arredondar(avaliacao.penalidadeOcupacao()));
        dto.setPenalidadeDestino(arredondar(avaliacao.penalidadeDestino()));
        dto.setPenalidadeEquipamento(arredondar(avaliacao.penalidadeEquipamento()));
        List<String> justificativas = new ArrayList<>();
        justificativas.add("Posicao operacional livre e compativel com carga, peso e altura.");
        justificativas.add("Distancia operacional estimada em " + dto.getDistancia() + " unidades.");
        justificativas.add("Ocupacao da pilha considerada em "
                + BigDecimal.valueOf(taxaOcupacao(avaliacao.posicao()) * 100)
                        .setScale(1, RoundingMode.HALF_UP) + "%.");
        justificativas.add("Equipamento " + dto.getEquipamentoId()
                + " selecionado por disponibilidade, carga e proximidade.");
        if (destinoCompativel(item, avaliacao.posicao())) {
            justificativas.add("Bloco proposto alinhado ao destino operacional informado.");
        }
        if (Boolean.TRUE.equals(item.getReefer())) {
            justificativas.add("Compatibilidade reefer validada.");
        }
        if (Boolean.TRUE.equals(item.getImo())) {
            justificativas.add("Compatibilidade IMO validada.");
        }
        if (Boolean.TRUE.equals(item.getOog())) {
            justificativas.add("Compatibilidade OOG validada.");
        }
        dto.setJustificativas(justificativas);
        return dto;
    }

    private boolean compativel(SchedulerContainerDto item, SchedulerPositionCandidateDto posicao) {
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()
                || posicao.isReservadaPorOutro() || !posicao.isAllocationCompativel()) {
            return false;
        }
        if (StringUtils.hasText(item.getCamadaAtual())
                && !normalizar(item.getCamadaAtual()).equals(normalizar(posicao.getCamada()))) {
            return false;
        }
        if (posicao.isOcupada()
                && (!StringUtils.hasText(posicao.getCodigoOcupante())
                || !posicao.getCodigoOcupante().equalsIgnoreCase(item.getCodigoContainer()))) {
            return false;
        }
        if (!posicao.getTiposCargaPermitidos().isEmpty()
                && StringUtils.hasText(item.getTipoCarga())
                && posicao.getTiposCargaPermitidos().stream()
                .noneMatch(tipo -> tipo.equalsIgnoreCase(item.getTipoCarga()))) {
            return false;
        }
        if (posicao.getPesoMaximoToneladas() != null && item.getPesoToneladas() != null
                && item.getPesoToneladas().compareTo(posicao.getPesoMaximoToneladas()) > 0) {
            return false;
        }
        if (posicao.getAlturaMaximaMetros() != null && item.getAlturaMetros() != null
                && item.getAlturaMetros().compareTo(posicao.getAlturaMaximaMetros()) > 0) {
            return false;
        }
        if (Boolean.TRUE.equals(item.getReefer()) && !posicao.isReeferPermitida()) {
            return false;
        }
        if (Boolean.TRUE.equals(item.getImo()) && !posicao.isImoPermitida()) {
            return false;
        }
        if (Boolean.TRUE.equals(item.getOog()) && !posicao.isOogPermitida()) {
            return false;
        }
        return posicao.getCapacidadePilha() == null
                || posicao.getOcupacaoPilha() == null
                || posicao.getOcupacaoPilha() < posicao.getCapacidadePilha();
    }

    private List<SchedulerEquipmentDto> equipamentosDisponiveis(
            SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        List<SchedulerEquipmentDto> operacionais = safe(requisicao.getEquipamentosOperacionais()).stream()
                .filter(Objects::nonNull)
                .filter(equipamento -> equipamento.isDisponivel() && !equipamento.isConflitoRecurso())
                .filter(equipamento -> StringUtils.hasText(equipamento.getEquipamentoId()))
                .sorted(comparadorEquipamento())
                .toList();
        if (!operacionais.isEmpty()) {
            return operacionais;
        }
        return safe(requisicao.getEquipamentosDisponiveis()).stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .map(codigo -> {
                    SchedulerEquipmentDto equipamento = new SchedulerEquipmentDto();
                    equipamento.setEquipamentoId(codigo.trim());
                    equipamento.setDisponivel(true);
                    equipamento.setProdutividadeMovimentosHora(BigDecimal.valueOf(20));
                    return equipamento;
                })
                .toList();
    }

    private Comparator<SchedulerEquipmentDto> comparadorEquipamento() {
        return Comparator
                .comparing(SchedulerEquipmentDto::getProdutividadeMovimentosHora,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SchedulerEquipmentDto::getTotalOrdens,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(equipamento -> normalizar(equipamento.getEquipamentoId()));
    }

    private SchedulerEquipmentDto selecionarEquipamento(
            SchedulerPositionCandidateDto posicao,
            List<SchedulerEquipmentDto> equipamentos) {
        return equipamentos.stream()
                .min(Comparator
                        .comparingDouble(equipamento -> custoEquipamento(posicao, equipamento))
                        .thenComparing(equipamento -> normalizar(equipamento.getEquipamentoId())))
                .orElseThrow(() -> new IllegalArgumentException("Nenhum equipamento disponivel para a posicao."));
    }

    private double custoEquipamento(
            SchedulerPositionCandidateDto posicao,
            SchedulerEquipmentDto equipamento) {
        int distancia = Math.abs(valor(equipamento.getLinhaAtual()) - posicao.getLinha())
                + Math.abs(valor(equipamento.getColunaAtual()) - posicao.getColuna());
        double produtividade = equipamento.getProdutividadeMovimentosHora() == null
                || equipamento.getProdutividadeMovimentosHora().signum() <= 0
                ? 20.0
                : equipamento.getProdutividadeMovimentosHora().doubleValue();
        int carga = valor(equipamento.getTotalOrdens());
        int prioridade = valor(equipamento.getPrioridadeWorkQueue());
        return distancia + (carga * 0.5) + (prioridade * 0.1) + (20.0 / produtividade);
    }

    private boolean destinoCompativel(
            SchedulerContainerDto item,
            SchedulerPositionCandidateDto posicao) {
        if (!StringUtils.hasText(item.getDestino()) || !StringUtils.hasText(posicao.getBloco())) {
            return true;
        }
        String destino = normalizar(item.getDestino());
        String bloco = normalizar(posicao.getBloco());
        return destino.equals(bloco) || destino.contains(bloco) || bloco.contains(destino);
    }

    private double taxaOcupacao(SchedulerPositionCandidateDto posicao) {
        if (posicao.getCapacidadePilha() == null || posicao.getCapacidadePilha() <= 0
                || posicao.getOcupacaoPilha() == null) {
            return 0.0;
        }
        return Math.min(1.0, posicao.getOcupacaoPilha().doubleValue()
                / posicao.getCapacidadePilha().doubleValue());
    }

    private int estimarRehandles(SchedulerPositionCandidateDto posicao) {
        int nivel = extrairNivel(posicao.getCamada());
        long ocupacao = posicao.getOcupacaoPilha() == null ? 0 : posicao.getOcupacaoPilha();
        return Math.max(0, nivel - 1) + Math.max(0, (int) ocupacao - nivel);
    }

    private int extrairNivel(String camada) {
        if (!StringUtils.hasText(camada)) {
            return 1;
        }
        String digitos = camada.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digitos)) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(digitos));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private double fatorUrgencia(SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        double horas = cutoffRestanteHoras(requisicao);
        if (horas <= 0) {
            return 1.75;
        }
        if (horas <= 6) {
            return 1.50;
        }
        if (horas <= 12) {
            return 1.25;
        }
        return 1.0;
    }

    private double cutoffRestanteHoras(SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        if (requisicao.getCutoffOperacional() == null) {
            return 0.0;
        }
        return Math.max(0.0, Duration.between(LocalDateTime.now(),
                requisicao.getCutoffOperacional()).toMinutes() / 60.0);
    }

    private double janelaOperacionalHoras(SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        if (requisicao.getNavio() == null
                || requisicao.getNavio().getEtaChegada() == null
                || requisicao.getNavio().getEtaPartida() == null) {
            return 0.0;
        }
        return Math.max(0.0, Duration.between(
                requisicao.getNavio().getEtaChegada(),
                requisicao.getNavio().getEtaPartida()).toMinutes() / 60.0);
    }

    private double peso(
            SchedulerPlanoOperacionalRequisicaoDto requisicao,
            String chave,
            double padrao) {
        BigDecimal configurado = requisicao.getPesosCriterios().get(chave);
        return configurado == null || configurado.signum() < 0
                ? padrao
                : configurado.doubleValue();
    }

    private int custoOriginal(SchedulerContainerDto item) {
        return Math.abs(valor(item.getLinha())) + Math.abs(valor(item.getColuna())) + 1;
    }

    private Map<String, Double> memoriaVazia() {
        Map<String, Double> memoria = new LinkedHashMap<>();
        memoria.put("distancia", 0.0);
        memoria.put("ocupacao", 0.0);
        memoria.put("rehandles", 0.0);
        memoria.put("destino", 0.0);
        memoria.put("equipamento", 0.0);
        memoria.put("sequencia", 0.0);
        memoria.put("dwellTime", 0.0);
        return memoria;
    }

    private void acumular(Map<String, Double> memoria, String chave, double valor) {
        memoria.compute(chave, (ignorado, atual) -> arredondar((atual == null ? 0.0 : atual) + valor));
    }

    private String assinatura(
            SchedulerPlanoOperacionalRequisicaoDto requisicao,
            List<SchedulerAssignmentDto> atribuicoes) {
        StringBuilder base = new StringBuilder();
        if (requisicao.getNavio() != null) {
            base.append(normalizar(requisicao.getNavio().getCodigoNavio())).append('|')
                    .append(requisicao.getNavio().getEtaChegada()).append('|')
                    .append(requisicao.getNavio().getEtaPartida()).append('|')
                    .append(requisicao.getCutoffOperacional());
        }
        atribuicoes.stream()
                .sorted(Comparator.comparing(SchedulerAssignmentDto::getSequenciaPlano))
                .forEach(item -> base.append('|')
                        .append(normalizar(item.getCodigoContainer())).append(':')
                        .append(item.getLinhaProposta()).append(':')
                        .append(item.getColunaProposta()).append(':')
                        .append(normalizar(item.getCamadaProposta())).append(':')
                        .append(normalizar(item.getEquipamentoId())));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(base.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte valor : digest) {
                hexadecimal.append(String.format(Locale.ROOT, "%02x", valor));
            }
            return hexadecimal.substring(0, 24);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao esta disponivel para assinar o plano.", ex);
        }
    }

    private String chave(SchedulerPositionCandidateDto posicao) {
        return posicao.getLinha() + "-" + posicao.getColuna() + "-" + normalizar(posicao.getCamada());
    }

    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private <T> List<T> safe(List<T> valores) {
        return valores == null ? List.of() : valores;
    }

    public record ResultadoOtimizacaoReal(
            List<SchedulerAssignmentDto> atribuicoes,
            Map<String, Double> memoriaCalculo,
            List<String> justificativas,
            String assinaturaEntrada,
            int rehandlesEstimados,
            int distanciaOriginal,
            int distanciaOtimizada,
            double pontuacaoTotal) {
    }

    private record Avaliacao(
            SchedulerPositionCandidateDto posicao,
            SchedulerEquipmentDto equipamento,
            double score,
            int distanciaMovimento,
            int distanciaBerco,
            double distanciaPonderada,
            double penalidadeOcupacao,
            int rehandles,
            double penalidadeRehandle,
            double penalidadeDestino,
            double penalidadeEquipamento,
            double penalidadeSequencia,
            double bonusDwell) {
    }
}
