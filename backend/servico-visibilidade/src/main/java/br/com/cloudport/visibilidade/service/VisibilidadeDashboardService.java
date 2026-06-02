package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.AlertaDTO;
import br.com.cloudport.visibilidade.dto.ConteinerBuscaDTO;
import br.com.cloudport.visibilidade.dto.DashboardAtualizacaoDTO;
import br.com.cloudport.visibilidade.dto.DashboardVisibilidadeDTO;
import br.com.cloudport.visibilidade.dto.NavioDetalhadoDTO;
import br.com.cloudport.visibilidade.dto.OcupacaoPatioDTO;
import br.com.cloudport.visibilidade.dto.TimelineEventoDTO;
import br.com.cloudport.visibilidade.dto.ThroughputGateDTO;
import br.com.cloudport.visibilidade.dto.ZonaOcupacaoDTO;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.entity.StatusNavio;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class VisibilidadeDashboardService {

    private static final String TOPICO_DASHBOARD = "/topic/dashboard/geral";

    private final StatusNavioRepository statusNavioRepository;
    private final CapacidadeYardRepository capacidadeYardRepository;
    private final ConteinerLocalizacaoRepository conteinerLocalizacaoRepository;
    private final HistoricoMovimentoRepository historicoMovimentoRepository;
    private final AlertaRepository alertaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public VisibilidadeDashboardService(StatusNavioRepository statusNavioRepository,
                                        CapacidadeYardRepository capacidadeYardRepository,
                                        ConteinerLocalizacaoRepository conteinerLocalizacaoRepository,
                                        HistoricoMovimentoRepository historicoMovimentoRepository,
                                        AlertaRepository alertaRepository,
                                        SimpMessagingTemplate messagingTemplate) {
        this.statusNavioRepository = statusNavioRepository;
        this.capacidadeYardRepository = capacidadeYardRepository;
        this.conteinerLocalizacaoRepository = conteinerLocalizacaoRepository;
        this.historicoMovimentoRepository = historicoMovimentoRepository;
        this.alertaRepository = alertaRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public DashboardVisibilidadeDTO obterDashboard() {
        DashboardVisibilidadeDTO dashboard = new DashboardVisibilidadeDTO();
        dashboard.setAtualizadoEm(LocalDateTime.now());
        dashboard.setAlertasAtivos(listarAlertas(null, null, "ativo"));
        dashboard.setNaviosEmOperacao(listarNavios(null));
        dashboard.setPatio(obterOcupacaoPatio());
        dashboard.setGate(obterThroughputGate());
        dashboard.setConteineresCriticos(buscarContainers(null, "retido", null, null, Pageable.ofSize(5)).getContent());
        return dashboard;
    }

    @Transactional(readOnly = true)
    public List<StatusNavioDTO> listarNavios(List<String> status) {
        Predicate<StatusNavio> filtro = navio -> true;
        if (status != null && !status.isEmpty()) {
            List<String> normalizados = status.stream()
                    .filter(StringUtils::hasText)
                    .map(this::normalizar)
                    .collect(Collectors.toList());
            filtro = navio -> normalizados.contains(normalizar(navio.getStatusOperacional()));
        }

        return statusNavioRepository.findAll().stream()
                .filter(filtro)
                .sorted(Comparator.comparing(StatusNavio::getDataAtualizacao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearNavioResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NavioDetalhadoDTO obterDetalhesNavio(String navioId) {
        StatusNavio navio = statusNavioRepository.findByNavioId(navioId)
                .orElseThrow(() -> new IllegalArgumentException("Navio não encontrado: " + navioId));
        NavioDetalhadoDTO detalhe = new NavioDetalhadoDTO();
        detalhe.setResumo(mapearNavioResumo(navio));
        detalhe.setAlertas(alertaRepository.findByEntidadeIdAndStatus(navioId, "ativo").stream()
                .map(this::mapearAlerta)
                .collect(Collectors.toList()));
        detalhe.setTimeline(criarTimelineNavio(navio));
        detalhe.setProximaAcao(calcularProximaAcao(navio));
        return detalhe;
    }

    @Transactional(readOnly = true)
    public OcupacaoPatioDTO obterOcupacaoPatio() {
        List<CapacidadeYard> zonas = capacidadeYardRepository.findAll().stream()
                .sorted(Comparator.comparing(CapacidadeYard::getZona, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        int capacidadeTotal = zonas.stream().mapToInt(zona -> Optional.ofNullable(zona.getCapacidadeTotal()).orElse(0)).sum();
        int ocupacaoAtual = zonas.stream().mapToInt(zona -> Optional.ofNullable(zona.getOcupacaoAtual()).orElse(0)).sum();
        double percentual = capacidadeTotal == 0 ? 0d : (ocupacaoAtual * 100.0) / capacidadeTotal;

        OcupacaoPatioDTO patio = new OcupacaoPatioDTO();
        patio.setCapacidadeTotal(capacidadeTotal);
        patio.setOcupacaoAtual(ocupacaoAtual);
        patio.setPercentualOcupacao(round(percentual));
        patio.setStatus(definirStatusPatio(percentual));
        patio.setBloqueioAutomaticoEm(percentual >= 95d ? "Imediato" : percentual >= 85d ? "1h 20min" : "Sem bloqueio iminente");
        patio.setDataAtualizacao(zonas.stream()
                .map(CapacidadeYard::getDataAtualizacao)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now()));
        patio.setZonas(zonas.stream().map(this::mapearZonaOcupacao).collect(Collectors.toList()));
        return patio;
    }

    @Transactional(readOnly = true)
    public ThroughputGateDTO obterThroughputGate() {
        LocalDate hoje = LocalDate.now();
        List<HistoricoMovimento> movimentos = historicoMovimentoRepository.findAll().stream()
                .filter(movimento -> movimento.getTimestamp() != null && movimento.getTimestamp().toLocalDate().equals(hoje))
                .collect(Collectors.toList());

        long entradas = movimentos.stream()
                .filter(movimento -> contemPalavra(movimento.getTipo(), "ENTRADA", "GATE", "CHEGADA"))
                .count();
        long saidas = movimentos.stream()
                .filter(movimento -> contemPalavra(movimento.getTipo(), "SAIDA", "SAÍDA", "RETIRADA", "PARTIDA"))
                .count();
        double movimentosHoje = movimentos.size();
        double desempenho = movimentosHoje == 0 ? 0d : round(Math.min(100d, (movimentosHoje / 1200d) * 100d));
        double tempoMedio = movimentosHoje == 0 ? 0d : round(movimentos.stream()
                .mapToLong(movimento -> movimento.getTimestamp().toEpochSecond(ZoneOffset.UTC))
                .average()
                .orElse(0d) % 60d);

        ThroughputGateDTO gate = new ThroughputGateDTO();
        gate.setEntradasHoje((int) entradas);
        gate.setSaidasHoje((int) saidas);
        gate.setMovimentosHoje(movimentos.size());
        gate.setDesempenhoPercentual(desempenho);
        gate.setTempoMedioProcessamentoMinutos(tempoMedio);
        gate.setStatus(desempenho >= 100d ? "ACIMA_DA_META" : desempenho >= 85d ? "NA_META" : "ABAIXO_DA_META");
        gate.setDataAtualizacao(LocalDateTime.now());
        return gate;
    }

    @Transactional(readOnly = true)
    public List<AlertaDTO> listarAlertas(List<String> severidades, List<String> tipos, String status) {
        return alertaRepository.findAll().stream()
                .filter(alerta -> filtrarString(alerta.getSeveridade(), severidades))
                .filter(alerta -> filtrarString(alerta.getTipo(), tipos))
                .filter(alerta -> !StringUtils.hasText(status) || normalizar(alerta.getStatus()).equals(normalizar(status)))
                .sorted(Comparator.comparing(Alerta::getDataGerada, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearAlerta)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertaDTO obterAlerta(Long alertaId) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não encontrado: " + alertaId));
        return mapearAlerta(alerta);
    }

    @Transactional
    public AlertaDTO resolverAlerta(Long alertaId, String motivo) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não encontrado: " + alertaId));
        alerta.setStatus("resolvido");
        alerta.setDataResolucao(LocalDateTime.now());
        if (StringUtils.hasText(motivo)) {
            alerta.setAcaoSugerida(escapar(alerta.getAcaoSugerida()) + " | Resolvido: " + escapar(motivo));
        }
        Alerta salvo = alertaRepository.save(alerta);
        publicarDashboard();
        return mapearAlerta(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ConteinerBuscaDTO> buscarContainers(String containerId, String statusAtual, String zona, String navioDestino, Pageable pageable) {
        List<ConteinerBuscaDTO> filtrados = conteinerLocalizacaoRepository.findAll().stream()
                .filter(conteiner -> filtrarContainerId(conteiner.getContainerId(), containerId))
                .filter(conteiner -> filtrarTexto(conteiner.getStatusAtual(), statusAtual))
                .filter(conteiner -> filtrarTexto(conteiner.getZona(), zona))
                .filter(conteiner -> filtrarTexto(conteiner.getNavioDestinoId(), navioDestino))
                .sorted(Comparator.comparing(ConteinerLocalizacao::getDataAtualizacao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearConteiner)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtrados.size());
        List<ConteinerBuscaDTO> conteudo = start >= filtrados.size() ? List.of() : filtrados.subList(start, end);
        return new PageImpl<>(conteudo, pageable, filtrados.size());
    }

    @Transactional(readOnly = true)
    public br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO rastrearContainer(String containerId) {
        ConteinerLocalizacao localizacao = conteinerLocalizacaoRepository.findByContainerId(containerId)
                .orElseThrow(() -> new IllegalArgumentException("Container não encontrado: " + containerId));

        List<HistoricoMovimento> historico = historicoMovimentoRepository.findByContainerIdOrderByTimestampDesc(containerId);
        br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO dto = new br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO();
        dto.setContainerId(localizacao.getContainerId());
        dto.setStatusAtual(localizacao.getStatusAtual());

        br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.LocalizacaoDTO localizacaoDTO = new br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.LocalizacaoDTO();
        localizacaoDTO.setTipo("ATUAL");
        localizacaoDTO.setZona(localizacao.getZona());
        localizacaoDTO.setPosicao(localizacao.getPosicao());
        br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.CoordenadasDTO coordenadas = new br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.CoordenadasDTO();
        coordenadas.setLatitude(localizacao.getLatitude());
        coordenadas.setLongitude(localizacao.getLongitude());
        localizacaoDTO.setCoordenadas(coordenadas);
        localizacaoDTO.setDataAtualizacao(localizacao.getDataAtualizacao());
        dto.setLocalizacaoAtual(localizacaoDTO);

        br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.ProximoDestinoDTO proximo = new br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.ProximoDestinoDTO();
        proximo.setTipo("NAVIO");
        proximo.setId(localizacao.getNavioDestinoId());
        proximo.setBerco(null);
        dto.setProximoDestino(proximo);

        dto.setRotaCompleta(historico.stream().map(this::mapearRota).collect(Collectors.toList()));
        dto.setMetricas(montarMetricas(localizacao, historico));
        return dto;
    }

    @Scheduled(fixedDelayString = "${visibilidade.dashboard.refresh-ms:30000}")
    public void publicarDashboard() {
        DashboardVisibilidadeDTO dashboard = obterDashboard();
        messagingTemplate.convertAndSend(TOPICO_DASHBOARD, new DashboardAtualizacaoDTO("DASHBOARD_ATUALIZADO", LocalDateTime.now(), dashboard));
    }

    @Transactional
    public void detectarAlertasAutomaticos() {
        detectarAtrasosNavios();
        detectarGargalosYard();
        publicarDashboard();
    }

    private void detectarAtrasosNavios() {
        statusNavioRepository.findAll().forEach(navio -> {
            Integer atraso = navio.getAtrasoMinutos();
            if (atraso == null || atraso <= 30) {
                return;
            }
            String entidadeId = navio.getNavioId();
            boolean jaExiste = alertaRepository.findByEntidadeIdAndStatus(entidadeId, "ativo").stream()
                    .anyMatch(alerta -> "ATRASO_NAVIO".equalsIgnoreCase(alerta.getTipo()));
            if (jaExiste) {
                return;
            }

            Alerta alerta = new Alerta();
            alerta.setTipo("ATRASO_NAVIO");
            alerta.setSeveridade(atraso >= 360 ? "critica" : atraso >= 120 ? "alta" : "media");
            alerta.setEntidadeId(entidadeId);
            alerta.setDescricao("Navio " + navio.getNomeNavio() + " atrasado em " + atraso + " minutos");
            alerta.setDataGerada(LocalDateTime.now());
            alerta.setStatus("ativo");
            alerta.setAcaoSugerida("Priorizar operação e revisar janela do berço.");
            alertaRepository.save(alerta);
        });
    }

    private void detectarGargalosYard() {
        capacidadeYardRepository.findAll().forEach(zona -> {
            Double percentual = Optional.ofNullable(zona.getPercentualOcupacao()).orElse(0d);
            if (percentual < 85d) {
                return;
            }
            String entidadeId = "YARD-" + zona.getZona();
            boolean jaExiste = alertaRepository.findByEntidadeIdAndStatus(entidadeId, "ativo").stream()
                    .anyMatch(alerta -> "GARGALO_YARD".equalsIgnoreCase(alerta.getTipo()));
            if (jaExiste) {
                return;
            }

            Alerta alerta = new Alerta();
            alerta.setTipo("GARGALO_YARD");
            alerta.setSeveridade(percentual >= 95d ? "critica" : "alta");
            alerta.setEntidadeId(entidadeId);
            alerta.setDescricao("Zona " + zona.getZona() + " com ocupação em " + round(percentual) + "%");
            alerta.setDataGerada(LocalDateTime.now());
            alerta.setStatus("ativo");
            alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saídas.");
            alertaRepository.save(alerta);
        });
    }

    private StatusNavioDTO mapearNavioResumo(StatusNavio navio) {
        StatusNavioDTO dto = new StatusNavioDTO();
        dto.setNavioId(escapar(navio.getNavioId()));
        dto.setNomeNavio(escapar(navio.getNomeNavio()));
        dto.setStatusOperacional(escapar(navio.getStatusOperacional()));

        StatusNavioDTO.EtaDTO etaDTO = new StatusNavioDTO.EtaDTO();
        etaDTO.setEstimado(navio.getEtaEstimado());
        etaDTO.setChegadaReal(navio.getChegadaReal());
        etaDTO.setAtraso(navio.getAtrasoMinutos());
        dto.setEtaCurrent(etaDTO);

        if (StringUtils.hasText(navio.getBercoAlocado())) {
            StatusNavioDTO.BercoDTO bercoDTO = new StatusNavioDTO.BercoDTO();
            bercoDTO.setNumero(escapar(navio.getBercoAlocado()));
            bercoDTO.setDataInicio(navio.getChegadaReal());
            bercoDTO.setDataPrevistaSaida(navio.getEtaEstimado());
            dto.setBercoAlocado(bercoDTO);
        }

        StatusNavioDTO.OperacoesDTO operacoesDTO = new StatusNavioDTO.OperacoesDTO();
        operacoesDTO.setConteineresADescarregar(1000);
        operacoesDTO.setConteineresDescarregados(estimativaMovimentos(navio));
        operacoesDTO.setPorcentagemCompleta(navio.getPorcentagemCompleta());
        operacoesDTO.setVelocidadeMov(28d);
        dto.setOperacoesEmAndamento(operacoesDTO);
        dto.setEquipamentosAlocados(List.of());
        dto.setAlertasNavio(alertaRepository.findByEntidadeIdAndStatus(navio.getNavioId(), "ativo").stream()
                .map(alerta -> {
                    StatusNavioDTO.AlertaResumoDTO resumo = new StatusNavioDTO.AlertaResumoDTO();
                    resumo.setId(alerta.getId());
                    resumo.setTipo(alerta.getTipo());
                    resumo.setSeveridade(alerta.getSeveridade());
                    resumo.setDescricao(alerta.getDescricao());
                    resumo.setDataGerada(alerta.getDataGerada());
                    return resumo;
                })
                .collect(Collectors.toList()));
        dto.setTimeline(criarTimelineNavio(navio));
        return dto;
    }

    private List<TimelineEventoDTO> criarTimelineNavio(StatusNavio navio) {
        List<TimelineEventoDTO> timeline = new ArrayList<>();
        if (navio.getEtaEstimado() != null) {
            timeline.add(timeline("ETA estimado", navio.getEtaEstimado(), "Chegada prevista ao porto"));
        }
        if (navio.getChegadaReal() != null) {
            timeline.add(timeline("Chegada real", navio.getChegadaReal(), "Navio atracado no berço"));
        }
        if (StringUtils.hasText(navio.getBercoAlocado())) {
            timeline.add(timeline("Berço alocado", navio.getDataAtualizacao(), navio.getBercoAlocado()));
        }
        timeline.add(timeline("Status operacional", navio.getDataAtualizacao(), navio.getStatusOperacional()));
        return timeline;
    }

    private TimelineEventoDTO timeline(String evento, LocalDateTime tempo, String detalhe) {
        TimelineEventoDTO dto = new TimelineEventoDTO();
        dto.setEvento(evento);
        dto.setTempo(tempo);
        dto.setDetalhe(detalhe);
        return dto;
    }

    private String calcularProximaAcao(StatusNavio navio) {
        Integer atraso = navio.getAtrasoMinutos();
        if (atraso != null && atraso > 30) {
            return "Priorizar operação e rever sequência do berço.";
        }
        if (StringUtils.hasText(navio.getBercoAlocado())) {
            return "Manter operação e monitorar progresso.";
        }
        return "Aguardando alocação de berço.";
    }

    private ZonaOcupacaoDTO mapearZonaOcupacao(CapacidadeYard zona) {
        ZonaOcupacaoDTO dto = new ZonaOcupacaoDTO();
        dto.setZona(zona.getZona());
        dto.setCapacidadeTotal(zona.getCapacidadeTotal());
        dto.setOcupacaoAtual(zona.getOcupacaoAtual());
        dto.setPercentualOcupacao(round(Optional.ofNullable(zona.getPercentualOcupacao()).orElse(0d)));
        dto.setEquipamentosDisponiveis(zona.getEquipamentosDisponiveis());
        dto.setDataAtualizacao(zona.getDataAtualizacao());
        Double percentual = Optional.ofNullable(zona.getPercentualOcupacao()).orElse(0d);
        dto.setStatus(percentual >= 95d ? "CRITICA" : percentual >= 85d ? "ATENCAO" : "NORMAL");
        return dto;
    }

    private String definirStatusPatio(double percentual) {
        if (percentual >= 95d) {
            return "CRITICO";
        }
        if (percentual >= 85d) {
            return "ATENCAO";
        }
        return "NORMAL";
    }

    private ConteinerBuscaDTO mapearConteiner(ConteinerLocalizacao conteiner) {
        ConteinerBuscaDTO dto = new ConteinerBuscaDTO();
        dto.setContainerId(escapar(conteiner.getContainerId()));
        dto.setStatusAtual(escapar(conteiner.getStatusAtual()));
        dto.setZona(escapar(conteiner.getZona()));
        dto.setPosicao(escapar(conteiner.getPosicao()));
        dto.setLatitude(conteiner.getLatitude());
        dto.setLongitude(conteiner.getLongitude());
        dto.setNavioDestinoId(escapar(conteiner.getNavioDestinoId()));
        dto.setDataAtualizacao(conteiner.getDataAtualizacao());
        return dto;
    }

    private br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.RotaDTO mapearRota(HistoricoMovimento movimento) {
        br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.RotaDTO dto = new br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.RotaDTO();
        dto.setSequencia(movimento.getId() == null ? null : movimento.getId().intValue());
        dto.setLocal(escapar(movimento.getLocalizacao()));
        dto.setTimestamp(movimento.getTimestamp());
        dto.setStatus(escapar(movimento.getTipo()));
        return dto;
    }

    private br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.MetricasDTO montarMetricas(ConteinerLocalizacao localizacao, List<HistoricoMovimento> historico) {
        br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.MetricasDTO metricas = new br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO.MetricasDTO();
        long horas = localizacao.getDataAtualizacao() == null
                ? 0L
                : java.time.Duration.between(localizacao.getDataAtualizacao(), LocalDateTime.now()).toHours();
        metricas.setTempoNoYard(horas + "h");
        metricas.setDataPrevisaoSaida(historico.stream()
                .map(HistoricoMovimento::getTimestamp)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(localizacao.getDataAtualizacao()));
        return metricas;
    }

    private int estimativaMovimentos(StatusNavio navio) {
        return Optional.ofNullable(navio.getPorcentagemCompleta()).map(Double::intValue).orElse(45);
    }

    private boolean filtrarContainerId(String valor, String filtro) {
        if (!StringUtils.hasText(filtro)) {
            return true;
        }
        if (!StringUtils.hasText(valor)) {
            return false;
        }
        String valorNormalizado = normalizar(valor);
        String filtroNormalizado = normalizar(filtro).replace("*", "");
        return filtro.contains("*")
                ? valorNormalizado.startsWith(filtroNormalizado)
                : valorNormalizado.contains(filtroNormalizado);
    }

    private boolean filtrarTexto(String valor, String filtro) {
        if (!StringUtils.hasText(filtro)) {
            return true;
        }
        return StringUtils.hasText(valor) && normalizar(valor).contains(normalizar(filtro));
    }

    private boolean filtrarString(String valor, List<String> filtros) {
        if (filtros == null || filtros.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(valor)) {
            return false;
        }
        String normalizado = normalizar(valor);
        return filtros.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizar)
                .anyMatch(normalizado::equals);
    }

    private boolean contemPalavra(String valor, String... palavras) {
        if (!StringUtils.hasText(valor)) {
            return false;
        }
        String normalizado = normalizar(valor);
        for (String palavra : palavras) {
            if (normalizado.contains(normalizar(palavra))) {
                return true;
            }
        }
        return false;
    }

    private AlertaDTO mapearAlerta(Alerta alerta) {
        AlertaDTO dto = new AlertaDTO();
        dto.setId(alerta.getId());
        dto.setTipo(escapar(alerta.getTipo()));
        dto.setSeveridade(escapar(alerta.getSeveridade()));
        dto.setEntidadeId(escapar(alerta.getEntidadeId()));
        dto.setDescricao(escapar(alerta.getDescricao()));
        dto.setDataGerada(alerta.getDataGerada());
        dto.setDataResolucao(alerta.getDataResolucao());
        dto.setStatus(escapar(alerta.getStatus()));
        dto.setAcaoSugerida(escapar(alerta.getAcaoSugerida()));
        return dto;
    }

    private double round(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private String normalizar(String valor) {
        return HtmlUtils.htmlEscape(String.valueOf(valor), "UTF-8")
                .normalize(Locale.ROOT)
                .toLowerCase(Locale.ROOT);
    }

    private String escapar(String valor) {
        if (!StringUtils.hasText(valor)) {
            return valor;
        }
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }
}
