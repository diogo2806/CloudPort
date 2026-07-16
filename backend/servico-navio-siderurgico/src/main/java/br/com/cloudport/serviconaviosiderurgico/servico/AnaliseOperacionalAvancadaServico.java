package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.WorkQueuePatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.TelemetriaYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.TelemetriaYardCliente.TelemetriaEquipamentoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.DetalheCheDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.DivergenciaOperacionalDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.GargaloOperacionalDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.QuayMonitorDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.VisaoBlocoPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.VisaoPoraoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoEstruturalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnaliseOperacionalAvancadaServico {

    private static final Set<String> STATUS_FILA_ATIVA = Set.of("ATIVA", "EM_EXECUCAO", "DISPATCHING");
    private static final Set<String> STATUS_ORDEM_EXECUCAO = Set.of("EM_EXECUCAO", "DISPATCHED", "CARRYING");
    private static final Set<String> STATUS_ORDEM_CONCLUIDA = Set.of("CONCLUIDA", "CONCLUIDO", "COMPLETED");

    private final VisitaNavioServico visitaServico;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final IntegracaoNavioPatioServico integracaoServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;
    private final TelemetriaYardCliente telemetriaYardCliente;
    private final ValidacaoEstruturalNavioServico validacaoEstruturalServico;

    public AnaliseOperacionalAvancadaServico(
            VisitaNavioServico visitaServico,
            ItemOperacaoNavioRepositorio itemRepositorio,
            IntegracaoNavioPatioServico integracaoServico,
            OrdemPatioYardCliente ordemPatioYardCliente,
            TelemetriaYardCliente telemetriaYardCliente,
            ValidacaoEstruturalNavioServico validacaoEstruturalServico
    ) {
        this.visitaServico = visitaServico;
        this.itemRepositorio = itemRepositorio;
        this.integracaoServico = integracaoServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
        this.telemetriaYardCliente = telemetriaYardCliente;
        this.validacaoEstruturalServico = validacaoEstruturalServico;
    }

    @Transactional(readOnly = true)
    public PainelOperacionalAvancadoDTO analisar(Long visitaId) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioId(visitaId);
        List<ReservaPatioNavioDTO> reservas = integracaoServico.listarReservasPatio(visitaId);
        List<OrdemPatioDaVisitaDTO> ordens = integracaoServico.listarOrdensPatio(visitaId);
        List<WorkQueuePatioYardDTO> workQueues = consultarWorkQueues(visitaId);
        List<TelemetriaEquipamentoYardDTO> telemetrias = consultarTelemetrias();
        List<DivergenciaOperacionalDTO> divergencias = compararPlanejadoExecutado(itens, ordens);
        List<GargaloOperacionalDTO> gargalos = preverGargalos(visita, itens, ordens, workQueues, telemetrias);
        ValidacaoEstruturalNavioDTO validacao = validacaoEstruturalServico.validar(visitaId, null);

        return new PainelOperacionalAvancadoDTO(
                visitaId,
                LocalDateTime.now(),
                montarVesselView(itens, divergencias),
                montarYardView(reservas, ordens, divergencias),
                montarCheDetail(workQueues, telemetrias),
                montarQuayMonitor(visita, itens, workQueues, gargalos),
                divergencias,
                gargalos,
                validacao
        );
    }

    private List<VisaoPoraoDTO> montarVesselView(
            List<ItemOperacaoNavio> itens,
            List<DivergenciaOperacionalDTO> divergencias
    ) {
        Map<Integer, PoraoAcumulador> porPorao = new LinkedHashMap<>();
        for (ItemOperacaoNavio item : itens) {
            Integer porao = item.getPoraoPlanejado() == null ? 0 : item.getPoraoPlanejado();
            PoraoAcumulador acumulador = porPorao.computeIfAbsent(porao, chave -> new PoraoAcumulador());
            acumulador.itensPlanejados++;
            acumulador.pesoPlanejado = acumulador.pesoPlanejado.add(valor(item.getPesoTotalToneladas()));
            if (item.getStatus() == StatusItemCarga.OPERADO) {
                acumulador.itensOperados++;
                acumulador.pesoOperado = acumulador.pesoOperado.add(valor(item.getPesoTotalToneladas()));
            }
            acumulador.alertas += divergencias.stream()
                    .filter(divergencia -> Objects.equals(divergencia.itemOperacaoNavioId(), item.getId()))
                    .count();
        }
        return porPorao.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> new VisaoPoraoDTO(
                        entrada.getKey(),
                        entrada.getValue().itensPlanejados,
                        entrada.getValue().itensOperados,
                        entrada.getValue().pesoPlanejado,
                        entrada.getValue().pesoOperado,
                        Math.toIntExact(entrada.getValue().alertas)
                ))
                .toList();
    }

    private List<VisaoBlocoPatioDTO> montarYardView(
            List<ReservaPatioNavioDTO> reservas,
            List<OrdemPatioDaVisitaDTO> ordens,
            List<DivergenciaOperacionalDTO> divergencias
    ) {
        Map<String, BlocoAcumulador> blocos = new LinkedHashMap<>();
        for (ReservaPatioNavioDTO reserva : reservas) {
            String bloco = normalizarBloco(reserva.bloco(), reserva.posicaoPatioId());
            BlocoAcumulador acumulador = blocos.computeIfAbsent(bloco, chave -> new BlocoAcumulador());
            if (reserva.status() == StatusReservaPatioNavio.ATIVA || reserva.status() == StatusReservaPatioNavio.CONSUMIDA) {
                acumulador.reservasAtivas++;
            }
        }
        for (OrdemPatioDaVisitaDTO ordem : ordens) {
            String bloco = normalizarBloco(ordem.destino(), ordem.posicaoPlanejada());
            BlocoAcumulador acumulador = blocos.computeIfAbsent(bloco, chave -> new BlocoAcumulador());
            String status = normalizar(ordem.statusOrdem());
            if (STATUS_ORDEM_CONCLUIDA.contains(status)) {
                acumulador.concluidas++;
            } else if (STATUS_ORDEM_EXECUCAO.contains(status)) {
                acumulador.emExecucao++;
            } else {
                acumulador.pendentes++;
            }
        }
        divergencias.stream()
                .filter(divergencia -> "PATIO".equals(divergencia.categoria()))
                .forEach(divergencia -> blocos
                        .computeIfAbsent(normalizarBloco(divergencia.planejado(), null), chave -> new BlocoAcumulador())
                        .divergentes++);
        return blocos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> new VisaoBlocoPatioDTO(
                        entrada.getKey(),
                        entrada.getValue().reservasAtivas,
                        entrada.getValue().pendentes,
                        entrada.getValue().emExecucao,
                        entrada.getValue().concluidas,
                        entrada.getValue().divergentes
                ))
                .toList();
    }

    private List<DetalheCheDTO> montarCheDetail(
            List<WorkQueuePatioYardDTO> workQueues,
            List<TelemetriaEquipamentoYardDTO> telemetrias
    ) {
        Map<String, TelemetriaEquipamentoYardDTO> porEquipamento = telemetrias.stream()
                .filter(telemetria -> StringUtils.hasText(telemetria.getEquipamento()))
                .collect(Collectors.toMap(
                        telemetria -> normalizar(telemetria.getEquipamento()),
                        telemetria -> telemetria,
                        this::maisRecente,
                        LinkedHashMap::new
                ));
        List<DetalheCheDTO> detalhes = new ArrayList<>();
        for (WorkQueuePatioYardDTO fila : workQueues) {
            String equipamento = StringUtils.hasText(fila.getEquipamento()) ? fila.getEquipamento() : "SEM_EQUIPAMENTO";
            TelemetriaEquipamentoYardDTO telemetria = porEquipamento.get(normalizar(equipamento));
            List<OrdemPatioYardRespostaDTO> jobs = Optional.ofNullable(fila.getJobList()).orElse(List.of());
            int emExecucao = (int) jobs.stream()
                    .map(OrdemPatioYardRespostaDTO::getStatusOrdem)
                    .map(this::normalizar)
                    .filter(STATUS_ORDEM_EXECUCAO::contains)
                    .count();
            detalhes.add(new DetalheCheDTO(
                    equipamento,
                    telemetria == null ? null : telemetria.getTipoEquipamento(),
                    telemetria == null ? fila.getStatus() : telemetria.getStatusOperacional(),
                    fila.getIdentificador(),
                    fila.getPow(),
                    fila.getPoolOperacional(),
                    jobs.size(),
                    emExecucao,
                    telemetria == null ? null : telemetria.getCapturadoEm(),
                    telemetria == null ? null : telemetria.getLatitude(),
                    telemetria == null ? null : telemetria.getLongitude(),
                    telemetria == null ? null : telemetria.getHeading(),
                    telemetria == null ? fila.getBlocoZona() : telemetria.getPosicaoMaisProxima(),
                    telemetria == null ? null : telemetria.getWorkInstructionAtualId()
            ));
        }
        return detalhes.stream().sorted(Comparator.comparing(DetalheCheDTO::equipamento)).toList();
    }

    private QuayMonitorDTO montarQuayMonitor(
            VisitaNavio visita,
            List<ItemOperacaoNavio> itens,
            List<WorkQueuePatioYardDTO> workQueues,
            List<GargaloOperacionalDTO> gargalos
    ) {
        int executados = (int) itens.stream().filter(item -> item.getStatus() == StatusItemCarga.OPERADO).count();
        int planejados = itens.size();
        long minutos = visita.getInicioOperacao() == null ? 0 : Duration.between(
                visita.getInicioOperacao(),
                visita.getFimOperacao() == null ? LocalDateTime.now() : visita.getFimOperacao()
        ).toMinutes();
        BigDecimal movimentosPorHora = minutos <= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(executados).multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(minutos), 2, RoundingMode.HALF_UP);
        LocalDateTime previsaoConclusao = null;
        if (movimentosPorHora.signum() > 0 && executados < planejados) {
            BigDecimal horas = BigDecimal.valueOf(planejados - executados)
                    .divide(movimentosPorHora, 2, RoundingMode.CEILING);
            previsaoConclusao = LocalDateTime.now().plusMinutes(horas.multiply(BigDecimal.valueOf(60)).longValue());
        }
        int filasAtivas = (int) workQueues.stream()
                .filter(fila -> STATUS_FILA_ATIVA.contains(normalizar(fila.getStatus())))
                .count();
        int equipamentos = (int) workQueues.stream()
                .map(WorkQueuePatioYardDTO::getEquipamento)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        String risco = gargalos.stream().anyMatch(gargalo -> "CRITICA".equals(gargalo.severidade()))
                ? "CRITICO"
                : gargalos.stream().anyMatch(gargalo -> "ALTA".equals(gargalo.severidade())) ? "ALTO" : "CONTROLADO";
        return new QuayMonitorDTO(
                StringUtils.hasText(visita.getBercoAtual()) ? visita.getBercoAtual() : visita.getBercoPrevisto(),
                visita.getFase().name(),
                planejados,
                executados,
                Math.max(0, planejados - executados),
                filasAtivas,
                equipamentos,
                movimentosPorHora,
                visita.getInicioOperacao(),
                previsaoConclusao,
                risco
        );
    }

    private List<DivergenciaOperacionalDTO> compararPlanejadoExecutado(
            List<ItemOperacaoNavio> itens,
            List<OrdemPatioDaVisitaDTO> ordens
    ) {
        Map<Long, OrdemPatioDaVisitaDTO> ordemPorItem = ordens.stream()
                .filter(ordem -> ordem.itemOperacaoNavioId() != null)
                .collect(Collectors.toMap(
                        OrdemPatioDaVisitaDTO::itemOperacaoNavioId,
                        ordem -> ordem,
                        (primeira, segunda) -> primeira
                ));
        List<DivergenciaOperacionalDTO> divergencias = new ArrayList<>();
        for (ItemOperacaoNavio item : itens) {
            if (diferente(item.getPoraoPlanejado(), item.getPoraoReal())) {
                divergencias.add(divergencia(item, "ESTIVA", item.getPoraoPlanejado(), item.getPoraoReal(), "ALTA", "Porao executado difere do plano de estiva."));
            }
            if (diferenteTexto(item.getPosicaoPlanejada(), item.getPosicaoReal())) {
                divergencias.add(divergencia(item, "ESTIVA", item.getPosicaoPlanejada(), item.getPosicaoReal(), "ALTA", "Posicao executada no navio difere da estiva planejada."));
            }
            if (diferenteTexto(item.getPosicaoPatioPlanejada(), item.getPosicaoPatioReal())) {
                divergencias.add(divergencia(item, "PATIO", item.getPosicaoPatioPlanejada(), item.getPosicaoPatioReal(), "MEDIA", "Posicao real no patio difere da reserva ou ordem planejada."));
            }
            if (item.getOrdemTrabalhoPatioId() != null && !ordemPorItem.containsKey(item.getId())) {
                divergencias.add(divergencia(item, "EXECUCAO", item.getOrdemTrabalhoPatioId(), null, "CRITICA", "A ordem vinculada ao item nao foi localizada no Yard."));
            }
        }
        return List.copyOf(divergencias);
    }

    private List<GargaloOperacionalDTO> preverGargalos(
            VisitaNavio visita,
            List<ItemOperacaoNavio> itens,
            List<OrdemPatioDaVisitaDTO> ordens,
            List<WorkQueuePatioYardDTO> workQueues,
            List<TelemetriaEquipamentoYardDTO> telemetrias
    ) {
        List<GargaloOperacionalDTO> gargalos = new ArrayList<>();
        long bloqueados = itens.stream().filter(item -> item.getStatus() == StatusItemCarga.BLOQUEADO).count();
        if (bloqueados > 0) {
            gargalos.add(gargalo("ITENS_BLOQUEADOS", "CARGA", "CRITICA", bloqueados, visita.getCutoffOperacional(),
                    "Existem itens bloqueados no fluxo operacional.", "Resolver holds e bloqueios antes do dispatch."));
        }
        long pendentes = ordens.stream()
                .filter(ordem -> !STATUS_ORDEM_CONCLUIDA.contains(normalizar(ordem.statusOrdem())))
                .count();
        int capacidadeFilas = workQueues.stream()
                .filter(fila -> STATUS_FILA_ATIVA.contains(normalizar(fila.getStatus())))
                .mapToInt(fila -> Math.max(1, fila.getTotalOrdens()))
                .sum();
        if (pendentes > 0 && capacidadeFilas == 0) {
            gargalos.add(gargalo("SEM_FILA_ATIVA", "WORK_QUEUE", "CRITICA", pendentes, visita.getInicioOperacao(),
                    "Ha ordens pendentes sem work queue ativa.", "Ativar fila e vincular POW, pool e equipamento."));
        } else if (pendentes > Math.max(10, capacidadeFilas * 2L)) {
            gargalos.add(gargalo("BACKLOG_WORK_QUEUE", "WORK_QUEUE", "ALTA", pendentes, visita.getEtd(),
                    "O backlog supera duas vezes a capacidade observada das filas.", "Rebalancear filas, prioridades e equipamentos."));
        }
        long semEquipamento = workQueues.stream().filter(fila -> !StringUtils.hasText(fila.getEquipamento())).count();
        if (semEquipamento > 0) {
            gargalos.add(gargalo("FILA_SEM_CHE", "EQUIPAMENTO", "ALTA", semEquipamento, visita.getInicioOperacao(),
                    "Existem work queues sem CHE alocado.", "Alocar equipamento disponivel ou redistribuir o pool."));
        }
        LocalDateTime limiteTelemetria = LocalDateTime.now().minusMinutes(5);
        long telemetriaAtrasada = telemetrias.stream()
                .filter(telemetria -> telemetria.getCapturadoEm() == null || telemetria.getCapturadoEm().isBefore(limiteTelemetria))
                .count();
        if (telemetriaAtrasada > 0) {
            gargalos.add(gargalo("TELEMETRIA_DESATUALIZADA", "VMT", "MEDIA", telemetriaAtrasada, LocalDateTime.now(),
                    "Leituras de CHE estao ha mais de cinco minutos sem atualizacao.", "Verificar VMT, GPS, rede e sequencia de mensagens."));
        }
        if (visita.getCutoffOperacional() != null
                && visita.getCutoffOperacional().isBefore(LocalDateTime.now().plusHours(2))
                && pendentes > 0) {
            gargalos.add(gargalo("RISCO_CUTOFF", "JANELA_OPERACIONAL", "ALTA", pendentes, visita.getCutoffOperacional(),
                    "O cutoff ocorre em menos de duas horas e ainda existem ordens pendentes.", "Priorizar movimentos criticos e ampliar cobertura de CHE."));
        }
        return gargalos.stream()
                .sorted(Comparator.comparingInt(gargalo -> ordemSeveridade(gargalo.severidade())))
                .toList();
    }

    private List<WorkQueuePatioYardDTO> consultarWorkQueues(Long visitaId) {
        try {
            return ordemPatioYardCliente.listarWorkQueuesDaVisita(visitaId);
        } catch (RuntimeException erro) {
            return List.of();
        }
    }

    private List<TelemetriaEquipamentoYardDTO> consultarTelemetrias() {
        try {
            return telemetriaYardCliente.listar();
        } catch (RuntimeException erro) {
            return List.of();
        }
    }

    private TelemetriaEquipamentoYardDTO maisRecente(
            TelemetriaEquipamentoYardDTO primeira,
            TelemetriaEquipamentoYardDTO segunda
    ) {
        if (primeira.getCapturadoEm() == null) return segunda;
        if (segunda.getCapturadoEm() == null) return primeira;
        return segunda.getCapturadoEm().isAfter(primeira.getCapturadoEm()) ? segunda : primeira;
    }

    private DivergenciaOperacionalDTO divergencia(
            ItemOperacaoNavio item,
            String categoria,
            Object planejado,
            Object executado,
            String severidade,
            String mensagem
    ) {
        return new DivergenciaOperacionalDTO(
                item.getId(),
                item.getCodigoLote(),
                categoria,
                texto(planejado),
                texto(executado),
                severidade,
                mensagem
        );
    }

    private GargaloOperacionalDTO gargalo(
            String codigo,
            String recurso,
            String severidade,
            long quantidade,
            LocalDateTime previstoPara,
            String causa,
            String acao
    ) {
        return new GargaloOperacionalDTO(codigo, recurso, severidade, Math.toIntExact(quantidade), previstoPara, causa, acao);
    }

    private int ordemSeveridade(String severidade) {
        return switch (severidade) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }

    private boolean diferente(Object planejado, Object realizado) {
        return planejado != null && realizado != null && !Objects.equals(planejado, realizado);
    }

    private boolean diferenteTexto(String planejado, String realizado) {
        return planejado != null && realizado != null && !planejado.equalsIgnoreCase(realizado);
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String normalizarBloco(String principal, String alternativo) {
        String valor = StringUtils.hasText(principal) ? principal : alternativo;
        if (!StringUtils.hasText(valor)) {
            return "NAO_DEFINIDO";
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        int separador = normalizado.indexOf('-');
        return separador > 0 ? normalizado.substring(0, separador) : normalizado;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static class PoraoAcumulador {
        private int itensPlanejados;
        private int itensOperados;
        private BigDecimal pesoPlanejado = BigDecimal.ZERO;
        private BigDecimal pesoOperado = BigDecimal.ZERO;
        private long alertas;
    }

    private static class BlocoAcumulador {
        private int reservasAtivas;
        private int pendentes;
        private int emExecucao;
        private int concluidas;
        private int divergentes;
    }
}
