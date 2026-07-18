package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.WorkQueuePatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OtimizacaoYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente.PosicaoPatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OtimizacaoGlobalNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.math.BigDecimal;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OtimizacaoGlobalNavioPatioServico {

    private final VisitaNavioServico visitaServico;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final ReservaPatioNavioServico reservaPatioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;
    private final PosicaoPatioYardCliente posicaoPatioYardCliente;
    private final OtimizacaoYardCliente otimizacaoYardCliente;
    private final EventoOperacionalStreamingServico streamingServico;
    private final ThreadLocal<PlanoPreparado> planoPreparado = new ThreadLocal<>();

    public OtimizacaoGlobalNavioPatioServico(
            VisitaNavioServico visitaServico,
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            ReservaPatioNavioServico reservaPatioServico,
            OrdemPatioYardCliente ordemPatioYardCliente,
            PosicaoPatioYardCliente posicaoPatioYardCliente,
            OtimizacaoYardCliente otimizacaoYardCliente,
            EventoOperacionalStreamingServico streamingServico
    ) {
        this.visitaServico = visitaServico;
        this.itemRepositorio = itemRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.reservaPatioServico = reservaPatioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
        this.posicaoPatioYardCliente = posicaoPatioYardCliente;
        this.otimizacaoYardCliente = otimizacaoYardCliente;
        this.streamingServico = streamingServico;
    }

    @Transactional(readOnly = true)
    public OtimizacaoGlobalNavioPatioDTO prepararPlano(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando) {
        OtimizacaoGlobalNavioPatioDTO resultado = otimizarInterno(visitaId, comando);
        planoPreparado.set(new PlanoPreparado(visitaId, resultado));
        return resultado;
    }

    public void limparPlanoPreparado() {
        planoPreparado.remove();
    }

    @Transactional(readOnly = true)
    public OtimizacaoGlobalNavioPatioDTO otimizar(Long visitaId) {
        PlanoPreparado preparado = planoPreparado.get();
        if (preparado != null && Objects.equals(preparado.visitaId(), visitaId)) {
            return preparado.resultado();
        }
        return otimizarInterno(visitaId, null);
    }

    @Transactional(readOnly = true)
    public OtimizacaoGlobalNavioPatioDTO otimizar(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando) {
        return otimizarInterno(visitaId, comando);
    }

    private OtimizacaoGlobalNavioPatioDTO otimizarInterno(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioId(visitaId).stream()
                .sorted(Comparator.comparing(
                        ItemOperacaoNavio::getSequenciaOperacional,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Map<Long, ReservaPatioNavioDTO> reservaPorItem = reservasComPosicao(visitaId);
        List<WorkQueuePatioYardDTO> workQueues = ordemPatioYardCliente.listarWorkQueuesDaVisita(visitaId);
        List<String> equipamentos = equipamentosReais(workQueues);
        if (equipamentos.isEmpty()) {
            throw new IllegalArgumentException("A otimizacao global exige ao menos um equipamento alocado em work queue real.");
        }

        Map<Long, Integer> prioridadePorOrdem = prioridadesPorOrdem(workQueues);
        List<Map<String, Object>> importacao = new ArrayList<>();
        List<Map<String, Object>> exportacao = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        Set<Long> itensConsiderados = new LinkedHashSet<>();
        int semPosicao = 0;
        for (ItemOperacaoNavio item : itens) {
            ReservaPatioNavioDTO reserva = reservaPorItem.get(item.getId());
            if (reserva == null || reserva.linha() == null || reserva.coluna() == null) {
                semPosicao++;
                continue;
            }
            Map<String, Object> carga = montarCarga(item, visita, reserva, prioridadePorOrdem);
            if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA) {
                importacao.add(carga);
                itensConsiderados.add(item.getId());
            } else if (item.getTipoMovimento() == TipoMovimentoNavio.EMBARQUE) {
                exportacao.add(carga);
                itensConsiderados.add(item.getId());
            } else {
                alertas.add("O item " + item.getCodigoLote()
                        + " e RESTOW e permanece fora do dual-cycle do Yard.");
            }
        }
        if (importacao.isEmpty() && exportacao.isEmpty()) {
            throw new IllegalArgumentException("Nao existem itens de embarque ou descarga com reserva real para otimizar.");
        }

        LocalDateTime chegada = primeiraData(visita.getAtb(), visita.getEtb(), visita.getEta());
        LocalDateTime partida = primeiraData(visita.getEtd(), visita.getFimOperacao(), visita.getAtd());
        if (chegada == null || partida == null || !partida.isAfter(chegada)) {
            throw new IllegalArgumentException("A otimizacao global exige janela real ou estimada valida entre chegada/atracacao e partida.");
        }
        String berco = StringUtils.hasText(visita.getBercoAtual())
                ? visita.getBercoAtual()
                : visita.getBercoPrevisto();
        if (!StringUtils.hasText(berco)) {
            throw new IllegalArgumentException("A otimizacao global exige berco atual ou previsto.");
        }

        Map<String, Object> navio = new LinkedHashMap<>();
        navio.put("codigoNavio", visita.getCodigoVisita());
        navio.put("nomeBerco", berco);
        navio.put("etaChegada", chegada);
        navio.put("etaPartida", partida);
        navio.put("quantidadeContainersImportacao", importacao.size());
        navio.put("quantidadeContainersExportacao", exportacao.size());
        navio.put("prioridade", visita.getFase().name());
        navio.put("observacoes", "Plano integrado gerado pelo modulo Navio Siderurgico.");

        List<PosicaoPatioYardDTO> posicoes = posicaoPatioYardCliente.listarPosicoes();
        Set<String> reservasProtegidas = reservasProtegidas(visitaId, itensConsiderados);
        List<Map<String, Object>> candidatos = posicoes.stream()
                .filter(Objects::nonNull)
                .filter(posicao -> posicao.getLinha() != null
                        && posicao.getColuna() != null
                        && StringUtils.hasText(posicao.getCamadaOperacional()))
                .map(posicao -> montarCandidato(posicao, reservasProtegidas))
                .toList();
        if (candidatos.isEmpty()) {
            throw new IllegalArgumentException("O mapa real do Yard nao retornou posicoes candidatas validas.");
        }

        Map<String, Object> requisicao = new LinkedHashMap<>();
        requisicao.put("navio", navio);
        requisicao.put("equipamentosDisponiveis", equipamentos);
        requisicao.put("equipamentosOperacionais", montarEquipamentos(workQueues));
        requisicao.put("containersImportacao", importacao);
        requisicao.put("containersExportacao", exportacao);
        requisicao.put("posicoesCandidatas", candidatos);
        requisicao.put("cutoffOperacional", visita.getCutoffOperacional());
        requisicao.put("pesosCriterios", comando == null ? Map.of() : comando.pesosEfetivos());

        Map<String, Object> plano = otimizacaoYardCliente.otimizar(requisicao);
        String status = semPosicao == 0 ? "OTIMIZADO_REAL" : "OTIMIZADO_REAL_PARCIALMENTE";
        if (semPosicao > 0) {
            alertas.add(semPosicao
                    + " item(ns) ficaram fora da otimizacao por ausencia de posicao real no patio.");
        }
        Object justificativas = plano == null ? null : plano.get("justificativas");
        if (justificativas instanceof List<?> lista) {
            lista.stream().map(String::valueOf).forEach(alertas::add);
        }
        OtimizacaoGlobalNavioPatioDTO resultado = new OtimizacaoGlobalNavioPatioDTO(
                visitaId,
                LocalDateTime.now(),
                equipamentos.size(),
                importacao.size(),
                exportacao.size(),
                semPosicao,
                status,
                List.copyOf(new LinkedHashSet<>(alertas)),
                plano
        );
        streamingServico.publicar(
                visitaId,
                "OTIMIZACAO_GLOBAL",
                "PLANO_GLOBAL_OTIMIZADO_REAL",
                Map.of(
                        "status", status,
                        "equipamentos", equipamentos.size(),
                        "importacao", importacao.size(),
                        "exportacao", exportacao.size(),
                        "itensSemPosicao", semPosicao,
                        "posicoesCandidatas", candidatos.size()
                )
        );
        return resultado;
    }

    private Map<String, Object> montarCarga(
            ItemOperacaoNavio item,
            VisitaNavio visita,
            ReservaPatioNavioDTO reserva,
            Map<Long, Integer> prioridadePorOrdem) {
        String observacoes = normalizar(item.getObservacoes());
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("codigoContainer", item.getCodigoLote());
        carga.put("linha", reserva.linha());
        carga.put("coluna", reserva.coluna());
        carga.put("camadaAtual", reserva.camada());
        carga.put("tipoCarga", item.getTipoCarga() == null ? null : item.getTipoCarga().name());
        carga.put("movimento", item.getTipoMovimento() == null ? null : item.getTipoMovimento().name());
        carga.put("destino", item.getDestinoPatio());
        carga.put("operador", visita.getLinhaOperadora());
        carga.put("pesoToneladas", item.getPesoUnitarioToneladas() == null
                ? item.getPesoTotalToneladas()
                : item.getPesoUnitarioToneladas());
        carga.put("alturaMetros", item.getAlturaCargaMetros());
        carga.put("imo", observacoes.contains("IMO") || observacoes.contains("IMDG"));
        carga.put("reefer", observacoes.contains("REEFER"));
        carga.put("oog", observacoes.contains("OOG") || observacoes.contains("OUT OF GAUGE"));
        carga.put("sequenciaOperacional", item.getSequenciaOperacional());
        carga.put("prioridadeWorkQueue", prioridadePorOrdem.get(item.getOrdemTrabalhoPatioId()));
        carga.put("dwellTimeHoras", dwellTimeHoras(item));
        return carga;
    }

    private Map<String, Object> montarCandidato(
            PosicaoPatioYardDTO posicao,
            Set<String> reservasProtegidas) {
        Map<String, Object> candidato = new LinkedHashMap<>();
        candidato.put("id", posicao.getId());
        candidato.put("linha", posicao.getLinha());
        candidato.put("coluna", posicao.getColuna());
        candidato.put("camada", posicao.getCamadaOperacional());
        candidato.put("bloco", posicao.getBloco());
        candidato.put("ocupada", posicao.isOcupada());
        candidato.put("codigoOcupante", posicao.getCodigoConteiner());
        candidato.put("bloqueada", posicao.isBloqueada());
        candidato.put("interditada", posicao.isInterditada());
        candidato.put("areaPermitida", posicao.isAreaPermitida());
        candidato.put("reservadaPorOutro", reservasProtegidas.contains(chave(posicao))
                || reservasProtegidas.contains(String.valueOf(posicao.getId())));
        candidato.put("allocationCompativel", true);
        candidato.put("reeferPermitida", true);
        candidato.put("imoPermitida", true);
        candidato.put("oogPermitida", true);
        candidato.put("tiposCargaPermitidos", posicao.getTiposCargaPermitidos());
        candidato.put("pesoMaximoToneladas", posicao.getPesoMaximoToneladas());
        candidato.put("alturaMaximaMetros", posicao.getAlturaMaximaMetros());
        candidato.put("capacidadePilha", posicao.getCapacidadePilha());
        candidato.put("ocupacaoPilha", posicao.getOcupacaoPilha());
        candidato.put("distanciaBerco", Math.abs(posicao.getLinha()) + Math.abs(posicao.getColuna()));
        return candidato;
    }

    private List<Map<String, Object>> montarEquipamentos(List<WorkQueuePatioYardDTO> workQueues) {
        Map<String, Long> ocorrencias = workQueues.stream()
                .map(WorkQueuePatioYardDTO::getEquipamento)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return workQueues.stream()
                .filter(queue -> StringUtils.hasText(queue.getEquipamento()))
                .map(queue -> {
                    String equipamento = queue.getEquipamento().trim();
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("equipamentoId", equipamento);
                    dto.put("statusOperacional", queue.getStatus());
                    dto.put("disponivel", equipamentoDisponivel(queue.getStatus()));
                    dto.put("conflitoRecurso", ocorrencias.getOrDefault(equipamento, 0L) > 1L);
                    dto.put("produtividadeMovimentosHora", BigDecimal.valueOf(
                            Math.max(5, 30 - Math.min(20, queue.getTotalOrdens()))));
                    dto.put("prioridadeWorkQueue", queue.getPrioridadeOperacional());
                    dto.put("totalOrdens", queue.getTotalOrdens());
                    OrdemPatioYardRespostaDTO primeira = queue.getJobList() == null
                            || queue.getJobList().isEmpty() ? null : queue.getJobList().get(0);
                    dto.put("linhaAtual", primeira == null ? null : primeira.getLinhaDestino());
                    dto.put("colunaAtual", primeira == null ? null : primeira.getColunaDestino());
                    return dto;
                })
                .toList();
    }

    private Map<Long, Integer> prioridadesPorOrdem(List<WorkQueuePatioYardDTO> workQueues) {
        Map<Long, Integer> resultado = new LinkedHashMap<>();
        for (WorkQueuePatioYardDTO queue : workQueues) {
            if (queue.getJobList() == null) {
                continue;
            }
            for (OrdemPatioYardRespostaDTO ordem : queue.getJobList()) {
                if (ordem != null && ordem.getId() != null) {
                    resultado.put(ordem.getId(), queue.getPrioridadeOperacional());
                }
            }
        }
        return resultado;
    }

    private Set<String> reservasProtegidas(Long visitaId, Set<Long> itensConsiderados) {
        LocalDateTime agora = LocalDateTime.now();
        Set<String> resultado = new LinkedHashSet<>();
        reservaRepositorio.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReservaPatioNavio.ATIVA)
                .filter(reserva -> reserva.getExpiraEm() == null || reserva.getExpiraEm().isAfter(agora))
                .filter(reserva -> !Objects.equals(visitaId, reserva.getVisitaNavioId())
                        || !itensConsiderados.contains(reserva.getItemOperacaoNavioId()))
                .forEach(reserva -> {
                    if (StringUtils.hasText(reserva.getPosicaoPatioId())) {
                        resultado.add(reserva.getPosicaoPatioId().trim().toUpperCase(Locale.ROOT));
                    }
                    resultado.add(chave(reserva));
                });
        return resultado;
    }

    private String chave(PosicaoPatioYardDTO posicao) {
        return posicao.getLinha() + "-" + posicao.getColuna() + "-"
                + normalizar(posicao.getCamadaOperacional());
    }

    private String chave(ReservaPosicaoPatioNavio reserva) {
        return reserva.getLinha() + "-" + reserva.getColuna() + "-"
                + normalizar(reserva.getCamada());
    }

    private boolean equipamentoDisponivel(String status) {
        String normalizado = normalizar(status);
        return !normalizado.contains("SUSPENS")
                && !normalizado.contains("BLOQUE")
                && !normalizado.contains("DESATIV")
                && !normalizado.contains("INDISPON");
    }

    private int dwellTimeHoras(ItemOperacaoNavio item) {
        if (item.getCriadoEm() == null) {
            return 0;
        }
        long horas = Math.max(0, Duration.between(item.getCriadoEm(), LocalDateTime.now()).toHours());
        return horas > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) horas;
    }

    private Map<Long, ReservaPatioNavioDTO> reservasComPosicao(Long visitaId) {
        Map<Long, ReservaPatioNavioDTO> porItem = new LinkedHashMap<>();
        reservaPatioServico.listar(visitaId).stream()
                .filter(reserva -> reserva.status() == StatusReservaPatioNavio.ATIVA
                        || reserva.status() == StatusReservaPatioNavio.CONSUMIDA)
                .filter(reserva -> reserva.itemOperacaoNavioId() != null)
                .forEach(reserva -> porItem.put(reserva.itemOperacaoNavioId(), reserva));
        return porItem;
    }

    private List<String> equipamentosReais(List<WorkQueuePatioYardDTO> workQueues) {
        return workQueues.stream()
                .filter(queue -> equipamentoDisponivel(queue.getStatus()))
                .map(WorkQueuePatioYardDTO::getEquipamento)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private LocalDateTime primeiraData(LocalDateTime... datas) {
        for (LocalDateTime data : datas) {
            if (Objects.nonNull(data)) {
                return data;
            }
        }
        return null;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    private record PlanoPreparado(
            Long visitaId,
            OtimizacaoGlobalNavioPatioDTO resultado) {
    }
}
