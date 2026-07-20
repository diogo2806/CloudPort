package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.scheduler.dto.YardImpactRespostaDto;
import br.com.cloudport.servicoyard.scheduler.dto.YardImpactRespostaDto.ImpactoBlocoDto;
import br.com.cloudport.servicoyard.scheduler.dto.YardImpactRespostaDto.ImpactoPowDto;
import br.com.cloudport.servicoyard.scheduler.dto.YardImpactRespostaDto.UnidadeImpactoDto;
import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.PlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.repositorio.PlanoPosicaoOperacionalRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class YardImpactServico {

    private static final int HORIZONTE_MINIMO_HORAS = 6;
    private static final int HORIZONTE_MAXIMO_HORAS = 24;
    private static final int WORK_INSTRUCTIONS_POR_CHE = 4;
    private static final double LIMITE_SATURACAO_PERCENTUAL = 85.0;
    private static final Set<EstadoPlanoPosicaoOperacional> ESTADOS_ATIVOS = EnumSet.of(
            EstadoPlanoPosicaoOperacional.TENTATIVO,
            EstadoPlanoPosicaoOperacional.DEFINITIVO,
            EstadoPlanoPosicaoOperacional.IMINENTE);

    private final PlanoPosicaoOperacionalRepositorio planoRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;

    public YardImpactServico(
            PlanoPosicaoOperacionalRepositorio planoRepositorio,
            PosicaoPatioRepositorio posicaoRepositorio,
            ConteinerPatioRepositorio conteinerRepositorio,
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            WorkQueuePatioRepositorio workQueueRepositorio,
            EquipamentoPatioRepositorio equipamentoRepositorio) {
        this.planoRepositorio = planoRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.workQueueRepositorio = workQueueRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
    }

    @Transactional(readOnly = true)
    public YardImpactRespostaDto projetar(Integer horizonteSolicitado) {
        int horizonteHoras = normalizarHorizonte(horizonteSolicitado);
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = inicio.plusHours(horizonteHoras);

        List<PlanoPosicaoOperacional> planos = planoRepositorio
                .findByEstadoInAndHorizonteFimAfterOrderByHorizonteInicioAsc(ESTADOS_ATIVOS, inicio)
                .stream()
                .filter(plano -> plano.getHorizonteInicio() == null || plano.getHorizonteInicio().isBefore(fim))
                .toList();
        List<PosicaoPatio> posicoes = posicaoRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc();
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAllByOrderByCodigoAsc();
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio.findAll().stream()
                .filter(this::ordemProjetavel)
                .toList();
        List<WorkQueuePatio> filas = workQueueRepositorio.findAll();
        List<EquipamentoPatio> equipamentos = equipamentoRepositorio.findAllByOrderByTipoEquipamentoAscIdentificadorAsc();

        Map<String, PlanoPosicaoOperacional> planoPorUnidade = planos.stream()
                .collect(Collectors.toMap(
                        plano -> normalizar(plano.getCodigoContainer()),
                        plano -> plano,
                        (primeiro, segundo) -> segundo,
                        LinkedHashMap::new));

        YardImpactRespostaDto resposta = new YardImpactRespostaDto();
        resposta.setGeradoEm(inicio);
        resposta.setHorizonteInicio(inicio);
        resposta.setHorizonteFim(fim);
        resposta.setHorizonteHoras(horizonteHoras);
        resposta.setTotalEntradas(contar(ordens, TipoMovimentoPatio.ALOCACAO));
        resposta.setTotalSaidas(contar(ordens, TipoMovimentoPatio.REMOCAO, TipoMovimentoPatio.LIBERACAO));
        resposta.setTotalRehandles(contar(ordens, TipoMovimentoPatio.REMANEJAMENTO, TipoMovimentoPatio.TRANSFERENCIA));
        resposta.setTotalReservas((int) posicoes.stream().filter(posicao -> posicao.possuiReservaAtiva(inicio)).count());
        resposta.setTotalWorkInstructions(ordens.size());

        int demandaChe = calcularDemandaChe(ordens.size());
        int cheDisponiveis = (int) equipamentos.stream()
                .filter(equipamento -> equipamento.getStatusOperacional() == StatusEquipamento.OPERACIONAL)
                .count();
        resposta.setDemandaChe(demandaChe);
        resposta.setCheDisponiveis(cheDisponiveis);
        resposta.setDeficitChe(Math.max(0, demandaChe - cheDisponiveis));
        resposta.setBlocos(montarBlocos(posicoes, conteineres, planos, ordens, planoPorUnidade, inicio));
        resposta.setPows(montarPows(filas, ordens, equipamentos));
        resposta.setUnidades(planos.stream().map(this::montarUnidade).toList());
        resposta.setAlertas(montarAlertas(resposta));
        return resposta;
    }

    private List<ImpactoBlocoDto> montarBlocos(
            List<PosicaoPatio> posicoes,
            List<ConteinerPatio> conteineres,
            List<PlanoPosicaoOperacional> planos,
            List<OrdemTrabalhoPatio> ordens,
            Map<String, PlanoPosicaoOperacional> planoPorUnidade,
            LocalDateTime referencia) {
        Set<String> blocos = new LinkedHashSet<>();
        posicoes.forEach(posicao -> blocos.add(bloco(posicao.getBloco())));
        planos.forEach(plano -> blocos.add(bloco(plano.getBloco())));

        List<ImpactoBlocoDto> resultado = new ArrayList<>();
        for (String codigoBloco : blocos) {
            int capacidade = (int) posicoes.stream()
                    .filter(posicao -> bloco(posicao.getBloco()).equals(codigoBloco))
                    .count();
            int ocupacaoAtual = (int) conteineres.stream()
                    .filter(conteiner -> conteiner.getPosicao() != null)
                    .filter(conteiner -> bloco(conteiner.getPosicao().getBloco()).equals(codigoBloco))
                    .count();
            int reservas = (int) posicoes.stream()
                    .filter(posicao -> bloco(posicao.getBloco()).equals(codigoBloco))
                    .filter(posicao -> posicao.possuiReservaAtiva(referencia))
                    .count();
            List<OrdemTrabalhoPatio> ordensBloco = ordens.stream()
                    .filter(ordem -> pertenceAoBloco(ordem, codigoBloco, planoPorUnidade))
                    .toList();
            int entradas = contar(ordensBloco, TipoMovimentoPatio.ALOCACAO);
            int saidas = contar(ordensBloco, TipoMovimentoPatio.REMOCAO, TipoMovimentoPatio.LIBERACAO);
            int rehandles = contar(ordensBloco, TipoMovimentoPatio.REMANEJAMENTO, TipoMovimentoPatio.TRANSFERENCIA);
            int propostas = (int) planos.stream()
                    .filter(plano -> bloco(plano.getBloco()).equals(codigoBloco))
                    .count();
            int ocupacaoProjetada = Math.max(0, ocupacaoAtual + reservas + propostas + entradas - saidas);
            double percentual = capacidade == 0
                    ? (ocupacaoProjetada > 0 ? 100.0 : 0.0)
                    : BigDecimal.valueOf((ocupacaoProjetada * 100.0) / capacidade)
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();

            ImpactoBlocoDto dto = new ImpactoBlocoDto();
            dto.setBloco(codigoBloco);
            dto.setCapacidadePosicoes(capacidade);
            dto.setReservasAtivas(reservas);
            dto.setMovimentosPrevistos(ordensBloco.size());
            dto.setEntradas(entradas);
            dto.setSaidas(saidas);
            dto.setRehandles(rehandles);
            dto.setOcupacaoProjetadaPercentual(percentual);
            dto.setSaturado(percentual >= LIMITE_SATURACAO_PERCENTUAL);
            resultado.add(dto);
        }
        return resultado.stream()
                .sorted(Comparator.comparing(ImpactoBlocoDto::getOcupacaoProjetadaPercentual).reversed()
                        .thenComparing(ImpactoBlocoDto::getBloco))
                .toList();
    }

    private List<ImpactoPowDto> montarPows(
            List<WorkQueuePatio> filas,
            List<OrdemTrabalhoPatio> ordens,
            List<EquipamentoPatio> equipamentos) {
        Map<Long, List<OrdemTrabalhoPatio>> ordensPorFila = ordens.stream()
                .filter(ordem -> ordem.getWorkQueueId() != null)
                .collect(Collectors.groupingBy(OrdemTrabalhoPatio::getWorkQueueId));
        Map<String, List<WorkQueuePatio>> filasPorPow = filas.stream()
                .collect(Collectors.groupingBy(
                        fila -> StringUtils.hasText(fila.getPow()) ? fila.getPow().trim() : "SEM_POW",
                        LinkedHashMap::new,
                        Collectors.toList()));
        Set<Long> equipamentosOperacionais = equipamentos.stream()
                .filter(equipamento -> equipamento.getStatusOperacional() == StatusEquipamento.OPERACIONAL)
                .map(EquipamentoPatio::getId)
                .collect(Collectors.toSet());

        List<ImpactoPowDto> resultado = new ArrayList<>();
        filasPorPow.forEach((pow, filasPow) -> {
            int totalOrdens = filasPow.stream()
                    .mapToInt(fila -> ordensPorFila.getOrDefault(fila.getId(), List.of()).size())
                    .sum();
            int demanda = calcularDemandaChe(totalOrdens);
            int associados = (int) filasPow.stream()
                    .map(WorkQueuePatio::getEquipamentoPatioId)
                    .filter(java.util.Objects::nonNull)
                    .filter(equipamentosOperacionais::contains)
                    .distinct()
                    .count();
            List<String> bloqueios = new ArrayList<>();
            if ("SEM_POW".equals(pow)) {
                bloqueios.add("POW não informado.");
            }
            if (filasPow.stream().anyMatch(fila -> !StringUtils.hasText(fila.getPoolOperacional()))) {
                bloqueios.add("Pool operacional ausente em uma ou mais filas.");
            }
            if (filasPow.stream().anyMatch(fila -> fila.getEquipamentoPatioId() == null)) {
                bloqueios.add("CHE real não associado em uma ou mais filas.");
            }
            if (demanda > associados) {
                bloqueios.add("Déficit de CHE para a demanda projetada.");
            }

            ImpactoPowDto dto = new ImpactoPowDto();
            dto.setPow(pow);
            dto.setWorkQueues(filasPow.size());
            dto.setWorkInstructions(totalOrdens);
            dto.setDemandaChe(demanda);
            dto.setCheAssociados(associados);
            dto.setDeficitChe(Math.max(0, demanda - associados));
            dto.setBloqueado(!bloqueios.isEmpty());
            dto.setMotivosBloqueio(bloqueios);
            resultado.add(dto);
        });
        return resultado.stream()
                .sorted(Comparator.comparing(ImpactoPowDto::getDeficitChe).reversed()
                        .thenComparing(ImpactoPowDto::getPow))
                .toList();
    }

    private UnidadeImpactoDto montarUnidade(PlanoPosicaoOperacional plano) {
        UnidadeImpactoDto dto = new UnidadeImpactoDto();
        dto.setPlanoId(plano.getId());
        dto.setCodigoContainer(plano.getCodigoContainer());
        dto.setBloco(bloco(plano.getBloco()));
        dto.setLinha(plano.getLinha());
        dto.setColuna(plano.getColuna());
        dto.setCamada(plano.getCamada());
        dto.setEstado(plano.getEstado());
        dto.setEquipamentoId(plano.getEquipamentoId());
        dto.setHorizonteInicio(plano.getHorizonteInicio());
        dto.setHorizonteFim(plano.getHorizonteFim());
        dto.setValidoAte(plano.getValidoAte());
        dto.setOrigem(plano.getOrigem());
        dto.setMotivo(plano.getMotivo());
        return dto;
    }

    private List<String> montarAlertas(YardImpactRespostaDto resposta) {
        List<String> alertas = new ArrayList<>();
        resposta.getBlocos().stream()
                .filter(ImpactoBlocoDto::isSaturado)
                .forEach(bloco -> alertas.add("Bloco " + bloco.getBloco()
                        + " com ocupação projetada de " + bloco.getOcupacaoProjetadaPercentual() + "%."));
        resposta.getPows().stream()
                .filter(ImpactoPowDto::isBloqueado)
                .forEach(pow -> alertas.add("POW " + pow.getPow() + " bloqueado: "
                        + String.join(" ", pow.getMotivosBloqueio())));
        if (resposta.getDeficitChe() > 0) {
            alertas.add("Déficit global de " + resposta.getDeficitChe() + " CHE no horizonte projetado.");
        }
        return alertas;
    }

    private boolean pertenceAoBloco(
            OrdemTrabalhoPatio ordem,
            String bloco,
            Map<String, PlanoPosicaoOperacional> planoPorUnidade) {
        PlanoPosicaoOperacional plano = planoPorUnidade.get(normalizar(ordem.getCodigoConteiner()));
        if (plano != null) {
            return bloco(plano.getBloco()).equals(bloco);
        }
        return bloco(ordem.getDestino()).equals(bloco);
    }

    private boolean ordemProjetavel(OrdemTrabalhoPatio ordem) {
        return ordem != null
                && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.CONCLUIDA
                && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.CANCELADA;
    }

    private int contar(List<OrdemTrabalhoPatio> ordens, TipoMovimentoPatio... tipos) {
        Set<TipoMovimentoPatio> aceitos = Set.of(tipos);
        return (int) ordens.stream().filter(ordem -> aceitos.contains(ordem.getTipoMovimento())).count();
    }

    private int calcularDemandaChe(int totalOrdens) {
        return totalOrdens == 0 ? 0 : (int) Math.ceil(totalOrdens / (double) WORK_INSTRUCTIONS_POR_CHE);
    }

    private int normalizarHorizonte(Integer horizonteSolicitado) {
        if (horizonteSolicitado == null) {
            return HORIZONTE_MINIMO_HORAS;
        }
        return Math.max(HORIZONTE_MINIMO_HORAS, Math.min(HORIZONTE_MAXIMO_HORAS, horizonteSolicitado));
    }

    private String bloco(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(java.util.Locale.ROOT) : "SEM_BLOCO";
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(java.util.Locale.ROOT) : "";
    }
}
