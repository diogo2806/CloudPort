package br.com.cloudport.servicoyard.recursos.servico;

import br.com.cloudport.servicoyard.recursos.dto.BercoResumoDTO;
import br.com.cloudport.servicoyard.recursos.dto.CalendarioBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.DiaCalendarioBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.EquipamentoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.EventoRecursosTempoRealDTO;
import br.com.cloudport.servicoyard.recursos.dto.ReservaBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.RespostaAlocacaoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.ResumoRecursosDTO;
import br.com.cloudport.servicoyard.recursos.dto.SolicitacaoAlocacaoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.SolicitacaoManutencaoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.ZonaArmazenagemDTO;
import br.com.cloudport.servicoyard.recursos.entidade.BercoPortuario;
import br.com.cloudport.servicoyard.recursos.entidade.EquipamentoBerco;
import br.com.cloudport.servicoyard.recursos.entidade.ReservaBerco;
import br.com.cloudport.servicoyard.recursos.entidade.StatusBerco;
import br.com.cloudport.servicoyard.recursos.entidade.StatusEquipamentoBerco;
import br.com.cloudport.servicoyard.recursos.entidade.StatusReservaBerco;
import br.com.cloudport.servicoyard.recursos.entidade.TipoReservaBerco;
import br.com.cloudport.servicoyard.recursos.entidade.ZonaArmazenagem;
import br.com.cloudport.servicoyard.recursos.repositorio.BercoPortuarioRepositorio;
import br.com.cloudport.servicoyard.recursos.repositorio.EquipamentoBercoRepositorio;
import br.com.cloudport.servicoyard.recursos.repositorio.ReservaBercoRepositorio;
import br.com.cloudport.servicoyard.recursos.repositorio.ZonaArmazenagemRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class ServicoRecursosService {

    private static final String TOPICO_ATUALIZACOES = "/topico/recursos";

    private final BercoPortuarioRepositorio bercoRepositorio;
    private final ReservaBercoRepositorio reservaRepositorio;
    private final ZonaArmazenagemRepositorio zonaRepositorio;
    private final EquipamentoBercoRepositorio equipamentoRepositorio;
    private final SimpMessagingTemplate messagingTemplate;

    public ServicoRecursosService(BercoPortuarioRepositorio bercoRepositorio,
                                  ReservaBercoRepositorio reservaRepositorio,
                                  ZonaArmazenagemRepositorio zonaRepositorio,
                                  EquipamentoBercoRepositorio equipamentoRepositorio,
                                  SimpMessagingTemplate messagingTemplate) {
        this.bercoRepositorio = bercoRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.zonaRepositorio = zonaRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<BercoResumoDTO> listarBercos() {
        return bercoRepositorio.findAllByOrderByCodigoAsc().stream()
                .map(berco -> converterBerco(berco, scoreOperacionalBase(berco), false, motivoOperacionalBase(berco)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalendarioBercoDTO> consultarCalendario(LocalDate inicio, int dias) {
        LocalDate inicioValido = inicio != null ? inicio : LocalDate.now();
        int totalDias = Math.max(7, Math.min(dias, 14));
        LocalDate fim = inicioValido.plusDays(totalDias);
        List<ReservaBerco> reservas = reservaRepositorio.findByChegadaPrevistaLessThanAndSaidaPrevistaGreaterThan(
                fim.atStartOfDay(),
                inicioValido.atStartOfDay()
        );

        return bercoRepositorio.findAllByOrderByCodigoAsc().stream()
                .map(berco -> new CalendarioBercoDTO(
                        berco.getCodigo(),
                        berco.getNome(),
                        montarDiasCalendario(inicioValido, totalDias, berco, reservas)
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumoRecursosDTO consultarResumo() {
        List<BercoPortuario> bercos = bercoRepositorio.findAllByOrderByCodigoAsc();
        List<ReservaBerco> reservas = reservaRepositorio.findAll();
        List<ZonaArmazenagem> zonas = zonaRepositorio.findAllByOrderByCodigoAsc();
        List<EquipamentoBerco> equipamentos = equipamentoRepositorio.findAllByOrderByBercoCodigoAscIdentificadorAsc();

        ResumoRecursosDTO resumo = new ResumoRecursosDTO();
        resumo.setTotalBercos(bercos.size());
        resumo.setBercosOperacionais((int) bercos.stream().filter(berco -> berco.getStatus() == StatusBerco.OPERACIONAL).count());
        resumo.setBercosEmManutencao((int) bercos.stream().filter(berco -> berco.getStatus() == StatusBerco.MANUTENCAO).count());
        resumo.setBercosBloqueados((int) bercos.stream().filter(berco -> berco.getStatus() == StatusBerco.BLOQUEADO).count());
        resumo.setReservasConfirmadas((int) reservas.stream().filter(reserva -> reserva.getStatus() == StatusReservaBerco.CONFIRMADA).count());
        resumo.setReservasPropostas((int) reservas.stream().filter(reserva -> reserva.getStatus() == StatusReservaBerco.PROPOSTA).count());
        resumo.setZonas(zonas.stream().map(this::converterZona).collect(Collectors.toList()));
        resumo.setEquipamentos(equipamentos.stream().map(this::converterEquipamento).collect(Collectors.toList()));
        resumo.setAlertas(construirAlertas(zonas, bercos));
        return resumo;
    }

    @Transactional(readOnly = true)
    public List<ReservaBercoDTO> listarReservas() {
        return reservaRepositorio.findAll().stream()
                .sorted(Comparator.comparing(ReservaBerco::getChegadaPrevista))
                .map(this::converterReserva)
                .collect(Collectors.toList());
    }

    @Transactional
    public RespostaAlocacaoBercoDTO recomendarOuConfirmarAlocacao(SolicitacaoAlocacaoBercoDTO solicitacao) {
        List<BercoPortuario> bercos = bercoRepositorio.findAllByOrderByCodigoAsc();
        if (bercos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum berço cadastrado.");
        }

        List<AvaliacaoBerco> avaliacoes = bercos.stream()
                .map(berco -> avaliarBerco(berco, solicitacao))
                .sorted(Comparator.comparingInt(AvaliacaoBerco::getScore).reversed())
                .collect(Collectors.toList());

        AvaliacaoBerco melhor = localizarMelhorAvaliacao(avaliacoes, solicitacao);
        if (melhor == null) {
            throw new IllegalArgumentException("Nenhum berço disponível para a janela informada.");
        }

        ReservaBerco reservaConfirmada = null;
        if (solicitacao.isConfirmar()) {
            reservaConfirmada = confirmarReserva(melhor.getBerco(), solicitacao, melhor);
        }

        RespostaAlocacaoBercoDTO resposta = new RespostaAlocacaoBercoDTO();
        resposta.setBercoRecomendado(converterBerco(melhor.getBerco(), melhor.getScore(), true, melhor.getMotivo()));
        resposta.setRanking(avaliacoes.stream()
                .limit(3)
                .map(avaliacao -> converterBerco(avaliacao.getBerco(), avaliacao.getScore(),
                        avaliacao.getBerco().getCodigo().equalsIgnoreCase(melhor.getBerco().getCodigo()),
                        avaliacao.getMotivo()))
                .collect(Collectors.toList()));
        if (reservaConfirmada != null) {
            resposta.setReservaConfirmada(converterReserva(reservaConfirmada));
        }
        resposta.setAlertas(construirAlertas(zonaRepositorio.findAll(), bercos));
        publicarAtualizacao(resposta);
        return resposta;
    }

    @Transactional
    public ReservaBercoDTO agendarManutencao(SolicitacaoManutencaoBercoDTO solicitacao) {
        BercoPortuario berco = bercoRepositorio.findByCodigoIgnoreCase(solicitacao.getBercoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Berço não encontrado."));
        if (solicitacao.getFim().isBefore(solicitacao.getInicio())) {
            throw new IllegalArgumentException("O fim da manutenção deve ser após o início.");
        }

        List<ReservaBerco> reservas = reservaRepositorio.findByBercoCodigoOrderByChegadaPrevistaAsc(berco.getCodigo());
        boolean conflito = reservas.stream().anyMatch(reserva -> sobrepoeManutencao(reserva, solicitacao.getInicio(), solicitacao.getFim()));
        if (conflito) {
            throw new IllegalArgumentException("Existe conflito com uma alocação já cadastrada.");
        }

        ReservaBerco reserva = new ReservaBerco();
        reserva.setBerco(berco);
        reserva.setNavioCodigo("MANUT-" + berco.getCodigo());
        reserva.setNavioNome(StringUtils.hasText(solicitacao.getObservacao()) ? solicitacao.getObservacao() : "Manutenção preventiva");
        reserva.setChegadaPrevista(solicitacao.getInicio().atStartOfDay());
        reserva.setSaidaPrevista(solicitacao.getFim().plusDays(1).atStartOfDay());
        reserva.setComprimentoNavio(0);
        reserva.setCaladoNavio(BigDecimal.ZERO);
        reserva.setGuinchesRequeridos(0);
        reserva.setTipoCarga("MANUTENCAO");
        reserva.setZonaArmazenagem(berco.getZonaPrimaria());
        reserva.setTipoReserva(TipoReservaBerco.MANUTENCAO);
        reserva.setStatus(StatusReservaBerco.CONFIRMADA);
        reserva.setScore(0);
        reserva.setMotivo("Manutenção agendada");
        reserva.setCriadoEm(LocalDateTime.now());
        reserva.setAtualizadoEm(LocalDateTime.now());
        ReservaBerco salva = reservaRepositorio.save(reserva);

        berco.setStatus(StatusBerco.MANUTENCAO);
        berco.setUltimaManutencao(solicitacao.getInicio());
        berco.setProximaManutencao(solicitacao.getFim().plusDays(60));
        bercoRepositorio.save(berco);
        publicarAtualizacao(null);
        return converterReserva(salva);
    }

    @Transactional(readOnly = true)
    public List<EquipamentoBercoDTO> listarEquipamentos() {
        return equipamentoRepositorio.findAllByOrderByBercoCodigoAscIdentificadorAsc().stream()
                .map(this::converterEquipamento)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BercoResumoDTO obterMelhorBercoPreview(SolicitacaoAlocacaoBercoDTO solicitacao) {
        List<BercoPortuario> bercos = bercoRepositorio.findAllByOrderByCodigoAsc();
        AvaliacaoBerco melhor = bercos.stream()
                .map(berco -> avaliarBerco(berco, solicitacao))
                .max(Comparator.comparingInt(AvaliacaoBerco::getScore))
                .orElse(null);
        if (melhor == null) {
            return null;
        }
        return converterBerco(melhor.getBerco(), melhor.getScore(), true, melhor.getMotivo());
    }

    private ReservaBerco confirmarReserva(BercoPortuario berco, SolicitacaoAlocacaoBercoDTO solicitacao, AvaliacaoBerco avaliacao) {
        List<ReservaBerco> reservas = reservaRepositorio.findByBercoCodigoOrderByChegadaPrevistaAsc(berco.getCodigo());
        boolean conflito = reservas.stream().anyMatch(reserva -> sobrepoe(reserva, solicitacao.getChegadaPrevista(), solicitacao.getSaidaPrevista()));
        if (conflito) {
            throw new IllegalArgumentException("Conflito detectado: o berço selecionado já possui reserva no período.");
        }
        if (berco.getStatus() != StatusBerco.OPERACIONAL) {
            throw new IllegalArgumentException("O berço selecionado não está operacional.");
        }
        validarCompatibilidade(berco, solicitacao);

        ReservaBerco reserva = new ReservaBerco();
        reserva.setBerco(berco);
        reserva.setNavioCodigo(solicitacao.getNavioCodigo());
        reserva.setNavioNome(solicitacao.getNavioNome());
        reserva.setChegadaPrevista(solicitacao.getChegadaPrevista());
        reserva.setSaidaPrevista(solicitacao.getSaidaPrevista());
        reserva.setComprimentoNavio(solicitacao.getComprimentoNavio());
        reserva.setCaladoNavio(solicitacao.getCaladoNavio());
        reserva.setGuinchesRequeridos(solicitacao.getGuinchesRequeridos());
        reserva.setTipoCarga(solicitacao.getTipoCarga());
        reserva.setZonaArmazenagem(solicitacao.getZonaArmazenagem());
        reserva.setTipoReserva(TipoReservaBerco.ALOCACAO);
        reserva.setStatus(StatusReservaBerco.CONFIRMADA);
        reserva.setScore(avaliacao.getScore());
        reserva.setMotivo(avaliacao.getMotivo());
        reserva.setCriadoEm(LocalDateTime.now());
        reserva.setAtualizadoEm(LocalDateTime.now());
        ReservaBerco salva = reservaRepositorio.save(reserva);
        atualizarZona(solicitacao.getZonaArmazenagem(), solicitacao.getToneladasPrevistas());
        return salva;
    }

    private AvaliacaoBerco localizarMelhorAvaliacao(List<AvaliacaoBerco> avaliacoes, SolicitacaoAlocacaoBercoDTO solicitacao) {
        if (StringUtils.hasText(solicitacao.getBercoPreferido())) {
            Optional<AvaliacaoBerco> preferido = avaliacoes.stream()
                    .filter(avaliacao -> avaliacao.getBerco().getCodigo().equalsIgnoreCase(solicitacao.getBercoPreferido()))
                    .findFirst();
            if (preferido.isPresent() && preferido.get().isDisponivel()) {
                return preferido.get();
            }
        }
        return avaliacoes.stream()
                .filter(AvaliacaoBerco::isDisponivel)
                .findFirst()
                .orElse(null);
    }

    private AvaliacaoBerco avaliarBerco(BercoPortuario berco, SolicitacaoAlocacaoBercoDTO solicitacao) {
        int score = 0;
        List<String> motivos = new ArrayList<>();

        if (berco.getStatus() == StatusBerco.OPERACIONAL) {
            score += 20;
            motivos.add("Operacional");
        } else if (berco.getStatus() == StatusBerco.MANUTENCAO) {
            score -= 100;
            motivos.add("Em manutenção");
        } else {
            score -= 80;
            motivos.add("Bloqueado");
        }

        if (berco.getComprimentoMetros() != null && berco.getComprimentoMetros() >= solicitacao.getComprimentoNavio()) {
            score += 25;
            motivos.add("Comprimento compatível");
        } else {
            score -= 50;
            motivos.add("Comprimento insuficiente");
        }

        if (berco.getCaladoMetros() != null && berco.getCaladoMetros().compareTo(solicitacao.getCaladoNavio()) >= 0) {
            score += 20;
            motivos.add("Calado compatível");
        } else {
            score -= 40;
            motivos.add("Calado insuficiente");
        }

        if (berco.getGuinchesPermanentes() != null && berco.getGuinchesPermanentes() >= solicitacao.getGuinchesRequeridos()) {
            score += 15;
            motivos.add("Guindastes suficientes");
        } else {
            score -= 20;
            motivos.add("Poucos guindastes");
        }

        if (compatibilidadePorTipo(berco, solicitacao.getTipoCarga())) {
            score += 15;
            motivos.add("Compatível com a carga");
        } else {
            score -= 30;
            motivos.add("Compatibilidade limitada");
        }

        boolean conflito = reservaRepositorio.findByBercoCodigoOrderByChegadaPrevistaAsc(berco.getCodigo()).stream()
                .anyMatch(reserva -> sobrepoe(reserva, solicitacao.getChegadaPrevista(), solicitacao.getSaidaPrevista()));
        if (conflito) {
            score -= 200;
            motivos.add("Conflito de agenda");
        } else {
            score += 25;
            motivos.add("Disponível no período");
        }

        ZonaArmazenagem zona = zonaRepositorio.findByCodigoIgnoreCase(solicitacao.getZonaArmazenagem()).orElse(null);
        if (zona != null && !zona.isBloqueada()) {
            score += 10;
            motivos.add("Zona livre");
        } else if (zona != null) {
            score -= 60;
            motivos.add("Zona bloqueada");
        }

        if (berco.getProximaManutencao() != null && !berco.getProximaManutencao().isAfter(solicitacao.getSaidaPrevista().toLocalDate())) {
            score -= 25;
            motivos.add("Manutenção próxima");
        }

        boolean disponivel = score > 0;
        return new AvaliacaoBerco(berco, score, disponivel, String.join(" | ", motivos));
    }

    private boolean compatibilidadePorTipo(BercoPortuario berco, String tipoCarga) {
        String tipoNormalizado = tipoCarga == null ? "" : tipoCarga.trim().toUpperCase(Locale.ROOT);
        switch (tipoNormalizado) {
            case "CONTAINER":
            case "CONTÊINER":
            case "CONTENEIR":
                return berco.isCompatContainer();
            case "BREAKBULK":
                return berco.isCompatBreakbulk();
            case "RORO":
                return berco.isCompatRoro();
            case "CARGA_GERAL":
            case "CARGA GERAL":
                return berco.isCompatCargaGeral();
            case "REEFER":
            case "FRIO":
                return berco.isCompatReefer();
            case "PERIGOSA":
                return berco.isCompatPerigosa();
            case "GRANEL":
                return berco.isCompatGranel();
            default:
                return true;
        }
    }

    private void validarCompatibilidade(BercoPortuario berco, SolicitacaoAlocacaoBercoDTO solicitacao) {
        if (berco.getComprimentoMetros() != null && berco.getComprimentoMetros() < solicitacao.getComprimentoNavio()) {
            throw new IllegalArgumentException("Conflito de alocação: o navio não cabe no berço informado.");
        }
        if (berco.getCaladoMetros() != null && berco.getCaladoMetros().compareTo(solicitacao.getCaladoNavio()) < 0) {
            throw new IllegalArgumentException("Conflito de alocação: calado incompatível com o berço.");
        }
        if (!compatibilidadePorTipo(berco, solicitacao.getTipoCarga())) {
            throw new IllegalArgumentException("Conflito de alocação: tipo de carga incompatível.");
        }
    }

    private boolean sobrepoe(ReservaBerco reserva, LocalDateTime inicio, LocalDateTime fim) {
        return reserva.getStatus() != StatusReservaBerco.CANCELADA
                && reserva.getChegadaPrevista().isBefore(fim)
                && reserva.getSaidaPrevista().isAfter(inicio);
    }

    private boolean sobrepoeManutencao(ReservaBerco reserva, LocalDate inicio, LocalDate fim) {
        return reserva.getStatus() != StatusReservaBerco.CANCELADA
                && reserva.getChegadaPrevista().toLocalDate().isBefore(fim.plusDays(1))
                && reserva.getSaidaPrevista().toLocalDate().isAfter(inicio.minusDays(1));
    }

    private void atualizarZona(String codigoZona, Integer toneladasPrevistas) {
        if (!StringUtils.hasText(codigoZona)) {
            return;
        }
        ZonaArmazenagem zona = zonaRepositorio.findByCodigoIgnoreCase(codigoZona)
                .orElseGet(() -> {
                    ZonaArmazenagem nova = new ZonaArmazenagem();
                    nova.setCodigo(codigoZona.toUpperCase(Locale.ROOT));
                    nova.setNome(codigoZona.toUpperCase(Locale.ROOT));
                    nova.setCapacidadeTotal(100);
                    nova.setOcupacaoAtual(0);
                    nova.setBloqueada(false);
                    nova.setAtualizadoEm(LocalDateTime.now());
                    return zonaRepositorio.save(nova);
                });
        int impacto = calcularImpactoOcupacao(toneladasPrevistas);
        int novaOcupacao = Math.min(zona.getCapacidadeTotal(), zona.getOcupacaoAtual() + impacto);
        zona.setOcupacaoAtual(novaOcupacao);
        zona.setBloqueada(percentual(novaOcupacao, zona.getCapacidadeTotal()) > 95d);
        zona.setAtualizadoEm(LocalDateTime.now());
        if (zona.isBloqueada()) {
            zona.setObservacao("Bloqueio automático por ocupação superior a 95%");
        }
        zonaRepositorio.save(zona);
    }

    private int calcularImpactoOcupacao(Integer toneladasPrevistas) {
        if (toneladasPrevistas == null || toneladasPrevistas <= 0) {
            return 8;
        }
        return Math.max(8, Math.min(25, toneladasPrevistas / 400));
    }

    private double percentual(int ocupacao, int capacidade) {
        if (capacidade <= 0) {
            return 0d;
        }
        return BigDecimal.valueOf(ocupacao)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacidade), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void publicarAtualizacao(RespostaAlocacaoBercoDTO alocacao) {
        ResumoRecursosDTO resumo = consultarResumo();
        EventoRecursosTempoRealDTO evento = new EventoRecursosTempoRealDTO("ATUALIZACAO_RECURSOS", resumo, alocacao);
        messagingTemplate.convertAndSend(TOPICO_ATUALIZACOES, evento);
    }

    private List<String> construirAlertas(List<ZonaArmazenagem> zonas, List<BercoPortuario> bercos) {
        List<String> alertas = new ArrayList<>();
        zonas.stream()
                .filter(ZonaArmazenagem::isBloqueada)
                .forEach(zona -> alertas.add("Zona " + zona.getCodigo() + " bloqueada por ocupação crítica."));
        bercos.stream()
                .filter(berco -> berco.getStatus() == StatusBerco.MANUTENCAO)
                .forEach(berco -> alertas.add("Berço " + berco.getCodigo() + " em manutenção preventiva."));
        if (alertas.isEmpty()) {
            alertas.add("Nenhum alerta crítico no momento.");
        }
        return alertas;
    }

    private List<DiaCalendarioBercoDTO> montarDiasCalendario(LocalDate inicio, int dias, BercoPortuario berco, List<ReservaBerco> reservas) {
        List<DiaCalendarioBercoDTO> resultado = new ArrayList<>();
        for (int indice = 0; indice < dias; indice++) {
            LocalDate data = inicio.plusDays(indice);
            ReservaBerco reservaDoDia = reservas.stream()
                    .filter(reserva -> reserva.getBerco().getCodigo().equalsIgnoreCase(berco.getCodigo()))
                    .filter(reserva -> !reserva.getChegadaPrevista().toLocalDate().isAfter(data)
                            && !reserva.getSaidaPrevista().toLocalDate().isBefore(data))
                    .findFirst()
                    .orElse(null);

            if (reservaDoDia == null) {
                resultado.add(new DiaCalendarioBercoDTO(data, "LIVRE", "Livre", null, null, null));
            } else {
                String status = reservaDoDia.getTipoReserva() == TipoReservaBerco.MANUTENCAO ? "MANUTENCAO" : "OCUPADO";
                String rotulo = reservaDoDia.getTipoReserva() == TipoReservaBerco.MANUTENCAO ? "Manutenção" : "Alocado";
                resultado.add(new DiaCalendarioBercoDTO(
                        data,
                        status,
                        rotulo,
                        reservaDoDia.getNavioCodigo(),
                        reservaDoDia.getNavioNome(),
                        reservaDoDia.getId()
                ));
            }
        }
        return resultado;
    }

    private BercoResumoDTO converterBerco(BercoPortuario berco, int score, boolean recomendado, String motivo) {
        BercoResumoDTO dto = new BercoResumoDTO();
        dto.setId(berco.getId());
        dto.setCodigo(escapar(berco.getCodigo()));
        dto.setNome(escapar(berco.getNome()));
        dto.setComprimentoMetros(berco.getComprimentoMetros());
        dto.setCaladoMetros(berco.getCaladoMetros());
        dto.setGuinchesPermanentes(berco.getGuinchesPermanentes());
        dto.setCapacidadeToneladasDia(berco.getCapacidadeToneladasDia());
        dto.setVoltagem(escapar(berco.getVoltagem()));
        dto.setAguaPotavel(berco.isAguaPotavel());
        dto.setEnergiaGenerica(berco.isEnergiaGenerica());
        dto.setIluminacaoNoturna(berco.isIluminacaoNoturna());
        dto.setSistemaSeguranca(berco.isSistemaSeguranca());
        dto.setCobertura(berco.isCobertura());
        dto.setZonaPrimaria(escapar(berco.getZonaPrimaria()));
        dto.setZonaSecundaria(escapar(berco.getZonaSecundaria()));
        dto.setDistanciaZonaMetros(berco.getDistanciaZonaMetros());
        dto.setTempoTransporteMinutos(berco.getTempoTransporteMinutos());
        dto.setDiasOperacao(escapar(berco.getDiasOperacao()));
        dto.setUltimaManutencao(berco.getUltimaManutencao());
        dto.setProximaManutencao(berco.getProximaManutencao());
        dto.setStatus(berco.getStatus());
        dto.setObservacoes(escapar(berco.getObservacoes()));
        dto.setScoreAtual(score);
        dto.setRecomendado(recomendado);
        dto.setMotivoRecomendacao(escapar(motivo));
        return dto;
    }

    private ReservaBercoDTO converterReserva(ReservaBerco reserva) {
        ReservaBercoDTO dto = new ReservaBercoDTO();
        dto.setId(reserva.getId());
        dto.setBercoCodigo(escapar(reserva.getBerco().getCodigo()));
        dto.setNavioCodigo(escapar(reserva.getNavioCodigo()));
        dto.setNavioNome(escapar(reserva.getNavioNome()));
        dto.setChegadaPrevista(reserva.getChegadaPrevista());
        dto.setSaidaPrevista(reserva.getSaidaPrevista());
        dto.setComprimentoNavio(reserva.getComprimentoNavio());
        dto.setCaladoNavio(reserva.getCaladoNavio());
        dto.setGuinchesRequeridos(reserva.getGuinchesRequeridos());
        dto.setTipoCarga(escapar(reserva.getTipoCarga()));
        dto.setZonaArmazenagem(escapar(reserva.getZonaArmazenagem()));
        dto.setTipoReserva(reserva.getTipoReserva());
        dto.setStatus(reserva.getStatus());
        dto.setScore(reserva.getScore());
        dto.setMotivo(escapar(reserva.getMotivo()));
        dto.setCriadoEm(reserva.getCriadoEm());
        return dto;
    }

    private ZonaArmazenagemDTO converterZona(ZonaArmazenagem zona) {
        ZonaArmazenagemDTO dto = new ZonaArmazenagemDTO();
        dto.setCodigo(escapar(zona.getCodigo()));
        dto.setNome(escapar(zona.getNome()));
        dto.setCapacidadeTotal(zona.getCapacidadeTotal());
        dto.setOcupacaoAtual(zona.getOcupacaoAtual());
        dto.setPercentualOcupacao(percentual(zona.getOcupacaoAtual(), zona.getCapacidadeTotal()));
        dto.setBloqueada(zona.isBloqueada());
        dto.setAtualizadoEm(zona.getAtualizadoEm());
        dto.setObservacao(escapar(zona.getObservacao()));
        return dto;
    }

    private EquipamentoBercoDTO converterEquipamento(EquipamentoBerco equipamento) {
        EquipamentoBercoDTO dto = new EquipamentoBercoDTO();
        dto.setIdentificador(escapar(equipamento.getIdentificador()));
        dto.setTipo(escapar(equipamento.getTipo()));
        dto.setBercoCodigo(escapar(equipamento.getBercoCodigo()));
        dto.setStatus(equipamento.getStatus());
        dto.setUltimaVerificacao(equipamento.getUltimaVerificacao());
        return dto;
    }

    private String escapar(String valor) {
        if (valor == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    private int scoreOperacionalBase(BercoPortuario berco) {
        if (berco.getStatus() == StatusBerco.OPERACIONAL) {
            return 80;
        }
        if (berco.getStatus() == StatusBerco.MANUTENCAO) {
            return -80;
        }
        return -50;
    }

    private String motivoOperacionalBase(BercoPortuario berco) {
        switch (berco.getStatus()) {
            case OPERACIONAL:
                return "Berço operacional e pronto para uso.";
            case MANUTENCAO:
                return "Berço em manutenção preventiva.";
            default:
                return "Berço bloqueado para operação.";
        }
    }

    private static class AvaliacaoBerco {
        private final BercoPortuario berco;
        private final int score;
        private final boolean disponivel;
        private final String motivo;

        private AvaliacaoBerco(BercoPortuario berco, int score, boolean disponivel, String motivo) {
            this.berco = berco;
            this.score = score;
            this.disponivel = disponivel;
            this.motivo = motivo;
        }

        public BercoPortuario getBerco() {
            return berco;
        }

        public int getScore() {
            return score;
        }

        public boolean isDisponivel() {
            return disponivel;
        }

        public String getMotivo() {
            return motivo;
        }
    }
}
