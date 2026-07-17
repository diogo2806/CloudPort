package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.AlertaDTO;
import br.com.cloudport.visibilidade.dto.ConteinerBuscaDTO;
import br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO;
import br.com.cloudport.visibilidade.dto.DashboardAtualizacaoDTO;
import br.com.cloudport.visibilidade.dto.DashboardVisibilidadeDTO;
import br.com.cloudport.visibilidade.dto.NavioDetalhadoDTO;
import br.com.cloudport.visibilidade.dto.OcupacaoPatioDTO;
import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.dto.ThroughputGateDTO;
import br.com.cloudport.visibilidade.dto.TimelineEventoDTO;
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
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VisibilidadeDashboardService {

    private static final String TOPICO_DASHBOARD = "/topic/dashboard/geral";
    private static final String STATUS_ALERTA_ATIVO = "ativo";
    private static final String STATUS_ALERTA_RESOLVIDO = "resolvido";

    private final StatusNavioRepository statusNavioRepository;
    private final CapacidadeYardRepository capacidadeYardRepository;
    private final ConteinerLocalizacaoRepository conteinerLocalizacaoRepository;
    private final HistoricoMovimentoRepository historicoMovimentoRepository;
    private final AlertaRepository alertaRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RastreamentoConteinerService rastreamentoConteinerService;
    private final AlertasService alertasService;
    private final int metaMovimentosGateDia;

    public VisibilidadeDashboardService(StatusNavioRepository statusNavioRepository,
                                         CapacidadeYardRepository capacidadeYardRepository,
                                         ConteinerLocalizacaoRepository conteinerLocalizacaoRepository,
                                         HistoricoMovimentoRepository historicoMovimentoRepository,
                                         AlertaRepository alertaRepository,
                                         SimpMessagingTemplate messagingTemplate,
                                         RastreamentoConteinerService rastreamentoConteinerService,
                                         AlertasService alertasService,
                                         @Value("${visibilidade.gate.meta-movimentos-dia:1200}") int metaMovimentosGateDia) {
        this.statusNavioRepository = statusNavioRepository;
        this.capacidadeYardRepository = capacidadeYardRepository;
        this.conteinerLocalizacaoRepository = conteinerLocalizacaoRepository;
        this.historicoMovimentoRepository = historicoMovimentoRepository;
        this.alertaRepository = alertaRepository;
        this.messagingTemplate = messagingTemplate;
        this.rastreamentoConteinerService = rastreamentoConteinerService;
        this.alertasService = alertasService;
        this.metaMovimentosGateDia = Math.max(1, metaMovimentosGateDia);
    }

    @Transactional(readOnly = true)
    public DashboardVisibilidadeDTO obterDashboard() {
        DashboardVisibilidadeDTO dashboard = new DashboardVisibilidadeDTO();
        dashboard.setAtualizadoEm(LocalDateTime.now());
        dashboard.setAlertasAtivos(listarAlertas(null, null, STATUS_ALERTA_ATIVO));
        dashboard.setNaviosEmOperacao(listarNavios(null));
        dashboard.setPatio(obterOcupacaoPatio());
        dashboard.setGate(obterThroughputGate());
        dashboard.setConteineresCriticos(
                buscarContainers(null, "retido", null, null, Pageable.ofSize(5)).getContent());
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
                .sorted(Comparator.comparing(StatusNavio::getDataAtualizacao,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearNavioResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NavioDetalhadoDTO obterDetalhesNavio(String navioId) {
        StatusNavio navio = statusNavioRepository.findByNavioId(navioId)
                .orElseThrow(() -> new IllegalArgumentException("Navio nao encontrado: " + navioId));

        NavioDetalhadoDTO detalhe = new NavioDetalhadoDTO();
        detalhe.setResumo(mapearNavioResumo(navio));
        detalhe.setAlertas(alertaRepository.findByEntidadeIdAndStatus(navioId, STATUS_ALERTA_ATIVO).stream()
                .map(this::mapearAlerta)
                .collect(Collectors.toList()));
        detalhe.setTimeline(criarTimelineNavio(navio));
        detalhe.setProximaAcao(calcularProximaAcao(navio));
        return detalhe;
    }

    @Transactional(readOnly = true)
    public OcupacaoPatioDTO obterOcupacaoPatio() {
        List<CapacidadeYard> zonas = capacidadeYardRepository.findAll().stream()
                .sorted(Comparator.comparing(CapacidadeYard::getZona,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        int capacidadeTotal = zonas.stream()
                .mapToInt(zona -> Optional.ofNullable(zona.getCapacidadeTotal()).orElse(0))
                .sum();
        int ocupacaoAtual = zonas.stream()
                .mapToInt(zona -> Optional.ofNullable(zona.getOcupacaoAtual()).orElse(0))
                .sum();
        double percentual = capacidadeTotal == 0 ? 0d : (ocupacaoAtual * 100.0) / capacidadeTotal;

        OcupacaoPatioDTO patio = new OcupacaoPatioDTO();
        patio.setCapacidadeTotal(capacidadeTotal);
        patio.setOcupacaoAtual(ocupacaoAtual);
        patio.setPercentualOcupacao(round(percentual));
        patio.setStatus(definirStatusPatio(percentual));
        patio.setBloqueioAutomaticoEm(descreverBloqueioPatio(percentual));
        patio.setDataAtualizacao(zonas.stream()
                .map(CapacidadeYard::getDataAtualizacao)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        patio.setZonas(zonas.stream().map(this::mapearZonaOcupacao).collect(Collectors.toList()));
        return patio;
    }

    @Transactional(readOnly = true)
    public ThroughputGateDTO obterThroughputGate() {
        LocalDate hoje = LocalDate.now();
        List<HistoricoMovimento> movimentosGate = historicoMovimentoRepository.findAll().stream()
                .filter(movimento -> movimento.getTimestamp() != null)
                .filter(movimento -> movimento.getTimestamp().toLocalDate().equals(hoje))
                .filter(this::movimentoDeGate)
                .sorted(Comparator.comparing(HistoricoMovimento::getTimestamp))
                .collect(Collectors.toList());

        long entradas = movimentosGate.stream().filter(this::entradaDeGate).count();
        long saidas = movimentosGate.stream().filter(this::saidaDeGate).count();
        int totalMovimentos = Math.toIntExact(entradas + saidas);
        double desempenho = round((totalMovimentos * 100.0) / metaMovimentosGateDia);

        ThroughputGateDTO gate = new ThroughputGateDTO();
        gate.setEntradasHoje(Math.toIntExact(entradas));
        gate.setSaidasHoje(Math.toIntExact(saidas));
        gate.setMovimentosHoje(totalMovimentos);
        gate.setDesempenhoPercentual(desempenho);
        gate.setTempoMedioProcessamentoMinutos(calcularTempoMedioGate(movimentosGate));
        gate.setStatus(definirStatusDesempenho(desempenho));
        gate.setDataAtualizacao(movimentosGate.stream()
                .map(HistoricoMovimento::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        return gate;
    }

    @Transactional(readOnly = true)
    public List<AlertaDTO> listarAlertas(List<String> severidades, List<String> tipos, String status) {
        return alertaRepository.findAll().stream()
                .filter(alerta -> filtrarString(alerta.getSeveridade(), severidades))
                .filter(alerta -> filtrarString(alerta.getTipo(), tipos))
                .filter(alerta -> !StringUtils.hasText(status)
                        || normalizar(alerta.getStatus()).equals(normalizar(status)))
                .sorted(Comparator.comparing(Alerta::getDataGerada,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearAlerta)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertaDTO obterAlerta(Long alertaId) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta nao encontrado: " + alertaId));
        return mapearAlerta(alerta);
    }

    @Transactional
    public AlertaDTO resolverAlerta(Long alertaId, String motivo) {
        if (!StringUtils.hasText(motivo)) {
            throw new IllegalArgumentException("Motivo e obrigatorio para resolver o alerta.");
        }

        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta nao encontrado: " + alertaId));
        alerta.setStatus(STATUS_ALERTA_RESOLVIDO);
        alerta.setDataResolucao(LocalDateTime.now());
        String acaoAnterior = StringUtils.hasText(alerta.getAcaoSugerida()) ? alerta.getAcaoSugerida() : "";
        alerta.setAcaoSugerida(acaoAnterior + (acaoAnterior.isEmpty() ? "" : " | ")
                + "Resolvido: " + motivo.trim());
        Alerta salvo = alertaRepository.save(alerta);
        publicarDashboard();
        return mapearAlerta(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ConteinerBuscaDTO> buscarContainers(String containerId,
                                                     String statusAtual,
                                                     String zona,
                                                     String navioDestino,
                                                     Pageable pageable) {
        List<ConteinerBuscaDTO> filtrados = conteinerLocalizacaoRepository.findAll().stream()
                .filter(conteiner -> filtrarContainerId(conteiner.getContainerId(), containerId))
                .filter(conteiner -> filtrarTexto(conteiner.getStatusAtual(), statusAtual))
                .filter(conteiner -> filtrarTexto(conteiner.getZona(), zona))
                .filter(conteiner -> filtrarTexto(conteiner.getNavioDestinoId(), navioDestino))
                .sorted(Comparator.comparing(ConteinerLocalizacao::getDataAtualizacao,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearConteiner)
                .collect(Collectors.toList());

        int inicio = Math.toIntExact(pageable.getOffset());
        int fim = Math.min(inicio + pageable.getPageSize(), filtrados.size());
        List<ConteinerBuscaDTO> conteudo = inicio >= filtrados.size()
                ? List.of()
                : filtrados.subList(inicio, fim);
        return new PageImpl<>(conteudo, pageable, filtrados.size());
    }

    @Transactional(readOnly = true)
    public ConteinerRastreamentoDTO rastrearContainer(String containerId) {
        return rastreamentoConteinerService.rastrearContainer(containerId);
    }

    public void publicarDashboard() {
        DashboardVisibilidadeDTO dashboard = obterDashboard();
        DashboardAtualizacaoDTO atualizacao = new DashboardAtualizacaoDTO(
                "DASHBOARD_ATUALIZADO", LocalDateTime.now(), dashboard);
        messagingTemplate.convertAndSend(TOPICO_DASHBOARD, atualizacao);
    }

    @Transactional
    public void detectarAlertasAutomaticos() {
        alertasService.detectarAtrasos();
        alertasService.detectarGargalos();
        publicarDashboard();
    }

    private StatusNavioDTO mapearNavioResumo(StatusNavio navio) {
        StatusNavioDTO dto = new StatusNavioDTO();
        dto.setNavioId(navio.getNavioId());
        dto.setNomeNavio(navio.getNomeNavio());
        dto.setStatusOperacional(navio.getStatusOperacional());

        StatusNavioDTO.EtaDTO etaDTO = new StatusNavioDTO.EtaDTO();
        etaDTO.setEstimado(navio.getEtaEstimado());
        etaDTO.setChegadaReal(navio.getChegadaReal());
        etaDTO.setAtraso(navio.getAtrasoMinutos());
        dto.setEtaCurrent(etaDTO);

        if (StringUtils.hasText(navio.getBercoAlocado())) {
            StatusNavioDTO.BercoDTO bercoDTO = new StatusNavioDTO.BercoDTO();
            bercoDTO.setNumero(navio.getBercoAlocado());
            bercoDTO.setDataInicio(navio.getChegadaReal());
            bercoDTO.setDataPrevistaSaida(null);
            dto.setBercoAlocado(bercoDTO);
        }

        StatusNavioDTO.OperacoesDTO operacoesDTO = new StatusNavioDTO.OperacoesDTO();
        operacoesDTO.setPorcentagemCompleta(navio.getPorcentagemCompleta());
        dto.setOperacoesEmAndamento(operacoesDTO);
        dto.setEquipamentosAlocados(List.of());
        dto.setAlertasNavio(alertaRepository
                .findByEntidadeIdAndStatus(navio.getNavioId(), STATUS_ALERTA_ATIVO).stream()
                .map(this::mapearAlertaResumo)
                .collect(Collectors.toList()));
        dto.setTimeline(criarTimelineResumoNavio(navio));
        return dto;
    }

    private StatusNavioDTO.AlertaResumoDTO mapearAlertaResumo(Alerta alerta) {
        StatusNavioDTO.AlertaResumoDTO resumo = new StatusNavioDTO.AlertaResumoDTO();
        resumo.setId(alerta.getId());
        resumo.setTipo(alerta.getTipo());
        resumo.setSeveridade(alerta.getSeveridade());
        resumo.setDescricao(alerta.getDescricao());
        resumo.setDataGerada(alerta.getDataGerada());
        return resumo;
    }

    private List<StatusNavioDTO.TimelineDTO> criarTimelineResumoNavio(StatusNavio navio) {
        List<StatusNavioDTO.TimelineDTO> timeline = new ArrayList<>();
        if (navio.getEtaEstimado() != null) {
            timeline.add(timelineResumo("ETA estimado", navio.getEtaEstimado()));
        }
        if (navio.getChegadaReal() != null) {
            timeline.add(timelineResumo("Chegada real", navio.getChegadaReal()));
        }
        if (StringUtils.hasText(navio.getBercoAlocado()) && navio.getDataAtualizacao() != null) {
            timeline.add(timelineResumo("Berco alocado", navio.getDataAtualizacao()));
        }
        if (StringUtils.hasText(navio.getStatusOperacional()) && navio.getDataAtualizacao() != null) {
            timeline.add(timelineResumo("Status operacional", navio.getDataAtualizacao()));
        }
        return timeline;
    }

    private StatusNavioDTO.TimelineDTO timelineResumo(String evento, LocalDateTime tempo) {
        StatusNavioDTO.TimelineDTO dto = new StatusNavioDTO.TimelineDTO();
        dto.setEvento(evento);
        dto.setTempo(tempo);
        return dto;
    }

    private List<TimelineEventoDTO> criarTimelineNavio(StatusNavio navio) {
        List<TimelineEventoDTO> timeline = new ArrayList<>();
        if (navio.getEtaEstimado() != null) {
            timeline.add(timeline("ETA estimado", navio.getEtaEstimado(), "Chegada prevista ao porto"));
        }
        if (navio.getChegadaReal() != null) {
            timeline.add(timeline("Chegada real", navio.getChegadaReal(), "Chegada confirmada"));
        }
        if (StringUtils.hasText(navio.getBercoAlocado()) && navio.getDataAtualizacao() != null) {
            timeline.add(timeline("Berco alocado", navio.getDataAtualizacao(), navio.getBercoAlocado()));
        }
        if (StringUtils.hasText(navio.getStatusOperacional()) && navio.getDataAtualizacao() != null) {
            timeline.add(timeline("Status operacional", navio.getDataAtualizacao(), navio.getStatusOperacional()));
        }
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
            return "Priorizar operacao e rever sequencia do berco.";
        }
        if (StringUtils.hasText(navio.getBercoAlocado())) {
            return "Manter operacao e monitorar progresso.";
        }
        return "Aguardando alocacao de berco.";
    }

    private ZonaOcupacaoDTO mapearZonaOcupacao(CapacidadeYard zona) {
        double percentual = calcularPercentualZona(zona);
        ZonaOcupacaoDTO dto = new ZonaOcupacaoDTO();
        dto.setZona(zona.getZona());
        dto.setCapacidadeTotal(zona.getCapacidadeTotal());
        dto.setOcupacaoAtual(zona.getOcupacaoAtual());
        dto.setPercentualOcupacao(round(percentual));
        dto.setEquipamentosDisponiveis(zona.getEquipamentosDisponiveis());
        dto.setDataAtualizacao(zona.getDataAtualizacao());
        dto.setStatus(percentual >= 95d ? "CRITICA" : percentual >= 85d ? "ATENCAO" : "NORMAL");
        return dto;
    }

    private double calcularPercentualZona(CapacidadeYard zona) {
        if (zona.getPercentualOcupacao() != null) {
            return zona.getPercentualOcupacao();
        }
        if (zona.getCapacidadeTotal() == null || zona.getCapacidadeTotal() <= 0
                || zona.getOcupacaoAtual() == null) {
            return 0d;
        }
        return (zona.getOcupacaoAtual() * 100.0) / zona.getCapacidadeTotal();
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

    private String descreverBloqueioPatio(double percentual) {
        if (percentual >= 95d) {
            return "Bloqueio operacional ativo";
        }
        if (percentual >= 85d) {
            return "Ao atingir 95% de ocupacao";
        }
        return "Sem bloqueio automatico ativo";
    }

    private boolean movimentoDeGate(HistoricoMovimento movimento) {
        return entradaDeGate(movimento) || saidaDeGate(movimento);
    }

    private boolean entradaDeGate(HistoricoMovimento movimento) {
        return MovimentoConteinerService.TIPO_ENTRADA_GATE.equalsIgnoreCase(movimento.getTipo());
    }

    private boolean saidaDeGate(HistoricoMovimento movimento) {
        return MovimentoConteinerService.TIPO_SAIDA_GATE.equalsIgnoreCase(movimento.getTipo());
    }

    private Double calcularTempoMedioGate(List<HistoricoMovimento> movimentosGate) {
        Map<String, LocalDateTime> entradasPendentes = new HashMap<>();
        List<Long> duracoesMinutos = new ArrayList<>();

        for (HistoricoMovimento movimento : movimentosGate) {
            if (!StringUtils.hasText(movimento.getContainerId()) || movimento.getTimestamp() == null) {
                continue;
            }
            if (entradaDeGate(movimento)) {
                entradasPendentes.put(movimento.getContainerId(), movimento.getTimestamp());
                continue;
            }
            if (saidaDeGate(movimento)) {
                LocalDateTime entrada = entradasPendentes.remove(movimento.getContainerId());
                if (entrada != null && !movimento.getTimestamp().isBefore(entrada)) {
                    duracoesMinutos.add(Duration.between(entrada, movimento.getTimestamp()).toMinutes());
                }
            }
        }

        if (duracoesMinutos.isEmpty()) {
            return null;
        }
        return round(duracoesMinutos.stream().mapToLong(Long::longValue).average().orElse(0d));
    }

    private String definirStatusDesempenho(double desempenho) {
        if (desempenho > 100d) {
            return "ACIMA_DA_META";
        }
        if (desempenho >= 85d) {
            return "NA_META";
        }
        return "ABAIXO_DA_META";
    }

    private ConteinerBuscaDTO mapearConteiner(ConteinerLocalizacao conteiner) {
        ConteinerBuscaDTO dto = new ConteinerBuscaDTO();
        dto.setContainerId(conteiner.getContainerId());
        dto.setStatusAtual(conteiner.getStatusAtual());
        dto.setZona(conteiner.getZona());
        dto.setPosicao(conteiner.getPosicao());
        dto.setLatitude(conteiner.getLatitude());
        dto.setLongitude(conteiner.getLongitude());
        dto.setNavioDestinoId(conteiner.getNavioDestinoId());
        dto.setDataAtualizacao(conteiner.getDataAtualizacao());
        return dto;
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

    private AlertaDTO mapearAlerta(Alerta alerta) {
        AlertaDTO dto = new AlertaDTO();
        dto.setId(alerta.getId());
        dto.setTipo(alerta.getTipo());
        dto.setSeveridade(alerta.getSeveridade());
        dto.setEntidadeId(alerta.getEntidadeId());
        dto.setDescricao(alerta.getDescricao());
        dto.setDataGerada(alerta.getDataGerada());
        dto.setDataResolucao(alerta.getDataResolucao());
        dto.setStatus(alerta.getStatus());
        dto.setAcaoSugerida(alerta.getAcaoSugerida());
        return dto;
    }

    private double round(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private String normalizar(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "";
        }
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }
}
