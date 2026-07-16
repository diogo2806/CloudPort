package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente.PosicaoPatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoGeracaoReservasPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReservaPatioNavioServico {

    private static final List<StatusReservaPatioNavio> STATUS_ATIVO = List.of(StatusReservaPatioNavio.ATIVA);

    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final VisitaNavioServico visitaServico;
    private final PosicaoPatioYardCliente posicaoPatioYardCliente;
    private final long duracaoReservaMinutos;

    public ReservaPatioNavioServico(
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            ItemOperacaoNavioRepositorio itemRepositorio,
            VisitaNavioServico visitaServico,
            PosicaoPatioYardCliente posicaoPatioYardCliente,
            @Value("${cloudport.integracao.yard.reserva-duracao-minutos:120}") long duracaoReservaMinutos
    ) {
        this.reservaRepositorio = reservaRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.visitaServico = visitaServico;
        this.posicaoPatioYardCliente = posicaoPatioYardCliente;
        this.duracaoReservaMinutos = Math.max(1, duracaoReservaMinutos);
    }

    @Transactional(readOnly = true)
    public List<ReservaPatioNavioDTO> listar(Long visitaId) {
        visitaServico.buscarEntidade(visitaId);
        return reservaRepositorio.findByVisitaNavioIdOrderByCriadoEmAsc(visitaId).stream()
                .map(ReservaPatioNavioDTO::de)
                .toList();
    }

    @Transactional
    public List<ReservaPatioNavioDTO> gerarReservasDaVisita(Long visitaId, ComandoGeracaoReservasPatioDTO comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        TipoReservaPatioNavio tipoReserva = comando == null ? TipoReservaPatioNavio.TENTATIVA : comando.tipoReservaEfetiva();
        boolean somentePendentes = comando == null || comando.somentePendentesEfetivo();
        String usuario = comando == null ? "sistema" : comando.usuario();
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(visitaId).stream()
                .filter(item -> item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA)
                .filter(item -> item.getStatus() != StatusItemCarga.OPERADO && item.getStatus() != StatusItemCarga.CANCELADO)
                .sorted(Comparator.comparing(ItemOperacaoNavio::getSequenciaOperacional, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<PosicaoPatioYardDTO> posicoes = posicoesDisponiveisDoYard();
        Set<String> posicoesReservadas = posicoesReservadasAtivas();
        Map<String, Long> reservasPorPilha = reservasAtivasPorPilha();

        List<ReservaPatioNavioDTO> reservas = itens.stream()
                .filter(item -> !somentePendentes || reservaAtiva(item) == null)
                .map(item -> reservarItem(item, tipoReserva, posicoes, posicoesReservadas, reservasPorPilha, null, usuario))
                .map(ReservaPatioNavioDTO::de)
                .toList();

        if (!reservas.isEmpty()) {
            visitaServico.registrarEvento(visita, null, "RESERVAS_PATIO_GERADAS",
                    reservas.size() + " reserva(s) de patio gerada(s) para descarga.",
                    usuario, null, String.valueOf(reservas.size()));
        }
        return reservas;
    }

    @Transactional
    public ReservaPosicaoPatioNavio reservarItem(ItemOperacaoNavio item, TipoReservaPatioNavio tipoReserva) {
        return reservarItem(
                item,
                tipoReserva,
                posicoesDisponiveisDoYard(),
                posicoesReservadasAtivas(),
                reservasAtivasPorPilha(),
                null,
                "sistema");
    }

    @Transactional
    public ReservaPosicaoPatioNavio replanejarItem(ItemOperacaoNavio item,
                                                    TipoReservaPatioNavio tipoReserva,
                                                    String usuario) {
        ReservaPosicaoPatioNavio anterior = reservaAtiva(item);
        if (anterior == null) {
            return reservarItem(item, tipoReserva);
        }

        List<PosicaoPatioYardDTO> posicoes = posicoesDisponiveisDoYard();
        Set<String> posicoesReservadas = posicoesReservadasAtivas();
        Map<String, Long> reservasPorPilha = reservasAtivasPorPilha();
        PosicaoPatioYardDTO novaPosicao = selecionarPosicao(item, posicoes, posicoesReservadas, reservasPorPilha);

        cancelar(anterior, "Reserva anterior compensada por replanejamento.", usuario, item);
        try {
            return criarReserva(item, tipoReserva, novaPosicao, anterior.getId(), usuario,
                    posicoesReservadas, reservasPorPilha);
        } catch (RuntimeException ex) {
            anterior.setStatus(StatusReservaPatioNavio.ATIVA);
            anterior.setMotivoCancelamento(null);
            reservaRepositorio.save(anterior);
            visitaServico.registrarEvento(item.getVisitaNavio(), item,
                    "RESERVA_PATIO_COMPENSACAO_REVERTIDA",
                    "Falha ao criar a nova reserva; a reserva anterior foi restaurada.",
                    usuario,
                    StatusReservaPatioNavio.CANCELADA.name(),
                    StatusReservaPatioNavio.ATIVA.name());
            throw ex;
        }
    }

    @Transactional
    public void consumirReservaDoItem(ItemOperacaoNavio item, String usuario) {
        reservaAtiva(item).ifPresent(reserva -> transicionar(
                reserva,
                StatusReservaPatioNavio.CONSUMIDA,
                null,
                usuario,
                item));
    }

    @Transactional
    public void cancelarReservaDoItem(ItemOperacaoNavio item, String motivo, String usuario) {
        reservaAtiva(item).ifPresent(reserva -> cancelar(reserva, motivo, usuario, item));
    }

    @Transactional
    public int cancelarReservasDaVisita(Long visitaId, String motivo, String usuario) {
        List<ReservaPosicaoPatioNavio> reservas = reservaRepositorio
                .findByVisitaNavioIdAndStatusOrderByCriadoEmAsc(visitaId, StatusReservaPatioNavio.ATIVA);
        for (ReservaPosicaoPatioNavio reserva : reservas) {
            ItemOperacaoNavio item = itemRepositorio.findById(reserva.getItemOperacaoNavioId()).orElse(null);
            cancelar(reserva, motivo, usuario, item);
        }
        return reservas.size();
    }

    @Transactional
    public int expirarReservasVencidas() {
        List<ReservaPosicaoPatioNavio> vencidas = reservaRepositorio
                .findByStatusAndExpiraEmLessThanEqualOrderByExpiraEmAsc(
                        StatusReservaPatioNavio.ATIVA,
                        LocalDateTime.now());
        for (ReservaPosicaoPatioNavio reserva : vencidas) {
            ItemOperacaoNavio item = itemRepositorio.findById(reserva.getItemOperacaoNavioId()).orElse(null);
            transicionar(reserva, StatusReservaPatioNavio.EXPIRADA,
                    "Prazo configurado da reserva expirado.", "sistema", item);
            if (item != null && item.getStatus() != StatusItemCarga.OPERADO && item.getStatus() != StatusItemCarga.CANCELADO) {
                if (Objects.equals(item.getPosicaoPatioPlanejada(), reserva.getPosicaoPatioId())) {
                    item.setPosicaoPatioPlanejada(null);
                }
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.NAO_GERADO);
                itemRepositorio.save(item);
            }
        }
        return vencidas.size();
    }

    private ReservaPosicaoPatioNavio reservarItem(ItemOperacaoNavio item,
                                                    TipoReservaPatioNavio tipoReserva,
                                                    List<PosicaoPatioYardDTO> posicoes,
                                                    Set<String> posicoesReservadas,
                                                    Map<String, Long> reservasPorPilha,
                                                    Long reservaAnteriorId,
                                                    String usuario) {
        ReservaPosicaoPatioNavio reservaExistente = reservaAtiva(item).orElse(null);
        if (reservaExistente != null && reservaAnteriorId == null) {
            return reservaExistente;
        }
        PosicaoPatioYardDTO posicao = selecionarPosicao(item, posicoes, posicoesReservadas, reservasPorPilha);
        return criarReserva(item, tipoReserva, posicao, reservaAnteriorId, usuario,
                posicoesReservadas, reservasPorPilha);
    }

    private ReservaPosicaoPatioNavio criarReserva(ItemOperacaoNavio item,
                                                   TipoReservaPatioNavio tipoReserva,
                                                   PosicaoPatioYardDTO posicao,
                                                   Long reservaAnteriorId,
                                                   String usuario,
                                                   Set<String> posicoesReservadas,
                                                   Map<String, Long> reservasPorPilha) {
        String identificador = posicao.identificador();
        validarPosicaoDisponivel(item, posicao, identificador, posicoesReservadas, reservasPorPilha);

        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        reserva.setVisitaNavioId(item.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(item.getId());
        reserva.setPosicaoPatioId(identificador);
        reserva.setTipoReserva(tipoReserva == null ? TipoReservaPatioNavio.TENTATIVA : tipoReserva);
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        reserva.setBloco(normalizarBloco(posicao.getBloco()));
        reserva.setLinha(posicao.getLinha());
        reserva.setColuna(posicao.getColuna());
        reserva.setCamada(posicao.getCamadaOperacional());
        reserva.setExpiraEm(LocalDateTime.now().plusMinutes(duracaoReservaMinutos));
        reserva.setReservaAnteriorId(reservaAnteriorId);
        ReservaPosicaoPatioNavio salva = reservaRepositorio.save(reserva);

        posicoesReservadas.add(identificador.toUpperCase(Locale.ROOT));
        reservasPorPilha.merge(chavePilha(posicao), 1L, Long::sum);
        item.setPosicaoPatioPlanejada(identificador);
        item.setDestinoPatio(StringUtils.hasText(posicao.getBloco()) ? posicao.getBloco() : identificador);
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
        itemRepositorio.save(item);

        visitaServico.registrarEvento(item.getVisitaNavio(), item,
                "RESERVA_PATIO_CRIADA",
                "Reserva " + salva.getId() + " criada para a posicao " + identificador
                        + " com validade ate " + salva.getExpiraEm() + ".",
                usuario,
                null,
                StatusReservaPatioNavio.ATIVA.name());
        return salva;
    }

    private java.util.Optional<ReservaPosicaoPatioNavio> reservaAtiva(ItemOperacaoNavio item) {
        java.util.Optional<ReservaPosicaoPatioNavio> reserva = reservaRepositorio
                .findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(item.getId(), STATUS_ATIVO);
        if (reserva.isPresent()
                && reserva.get().getExpiraEm() != null
                && !reserva.get().getExpiraEm().isAfter(LocalDateTime.now())) {
            transicionar(reserva.get(), StatusReservaPatioNavio.EXPIRADA,
                    "Prazo configurado da reserva expirado.", "sistema", item);
            return java.util.Optional.empty();
        }
        return reserva;
    }

    private List<PosicaoPatioYardDTO> posicoesDisponiveisDoYard() {
        List<PosicaoPatioYardDTO> posicoes = posicaoPatioYardCliente.listarPosicoes().stream()
                .filter(Objects::nonNull)
                .filter(posicao -> posicao.getId() != null)
                .filter(posicao -> posicao.getLinha() != null && posicao.getColuna() != null)
                .filter(posicao -> StringUtils.hasText(posicao.getCamadaOperacional()))
                .sorted(Comparator.comparing(PosicaoPatioYardDTO::getBloco, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(PosicaoPatioYardDTO::getLinha)
                        .thenComparing(PosicaoPatioYardDTO::getColuna)
                        .thenComparing(PosicaoPatioYardDTO::getCamadaOperacional))
                .toList();
        if (posicoes.isEmpty()) {
            throw new IllegalArgumentException("O mapa real do patio nao possui posicoes cadastradas para reserva.");
        }
        return posicoes;
    }

    private Set<String> posicoesReservadasAtivas() {
        Set<String> reservadas = new HashSet<>();
        reservaRepositorio.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReservaPatioNavio.ATIVA)
                .filter(reserva -> reserva.getExpiraEm() == null || reserva.getExpiraEm().isAfter(LocalDateTime.now()))
                .map(ReservaPosicaoPatioNavio::getPosicaoPatioId)
                .filter(StringUtils::hasText)
                .map(valor -> valor.trim().toUpperCase(Locale.ROOT))
                .forEach(reservadas::add);
        return reservadas;
    }

    private Map<String, Long> reservasAtivasPorPilha() {
        Map<String, Long> reservas = new HashMap<>();
        reservaRepositorio.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReservaPatioNavio.ATIVA)
                .filter(reserva -> reserva.getExpiraEm() == null || reserva.getExpiraEm().isAfter(LocalDateTime.now()))
                .filter(reserva -> reserva.getLinha() != null && reserva.getColuna() != null)
                .forEach(reserva -> reservas.merge(
                        chavePilha(reserva.getLinha(), reserva.getColuna()),
                        1L,
                        Long::sum));
        return reservas;
    }

    private PosicaoPatioYardDTO selecionarPosicao(ItemOperacaoNavio item,
                                                   List<PosicaoPatioYardDTO> posicoes,
                                                   Set<String> reservadas,
                                                   Map<String, Long> reservasPorPilha) {
        String preferida = normalizar(item.getPosicaoPatioPlanejada());
        if (StringUtils.hasText(preferida)) {
            PosicaoPatioYardDTO encontrada = posicoes.stream()
                    .filter(posicao -> correspondePreferencia(posicao, preferida))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A posicao planejada " + preferida + " nao existe no mapa real do patio."));
            validarPosicaoDisponivel(item, encontrada, encontrada.identificador(), reservadas, reservasPorPilha);
            return encontrada;
        }

        return posicoes.stream()
                .filter(posicao -> motivoIndisponibilidade(item, posicao, posicao.identificador(), reservadas, reservasPorPilha) == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nao ha posicao livre, permitida, compativel e com capacidade para a reserva."));
    }

    private boolean correspondePreferencia(PosicaoPatioYardDTO posicao, String preferida) {
        String porId = normalizar(posicao.identificador());
        String porCoordenada = normalizar(posicao.getLinha() + "-" + posicao.getColuna() + "-" + posicao.getCamadaOperacional());
        return preferida.equals(porId) || preferida.equals(porCoordenada);
    }

    private void validarPosicaoDisponivel(ItemOperacaoNavio item,
                                           PosicaoPatioYardDTO posicao,
                                           String identificador,
                                           Set<String> reservadas,
                                           Map<String, Long> reservasPorPilha) {
        String motivo = motivoIndisponibilidade(item, posicao, identificador, reservadas, reservasPorPilha);
        if (motivo != null) {
            throw new IllegalArgumentException(motivo);
        }
    }

    private String motivoIndisponibilidade(ItemOperacaoNavio item,
                                            PosicaoPatioYardDTO posicao,
                                            String identificador,
                                            Set<String> reservadas,
                                            Map<String, Long> reservasPorPilha) {
        if (posicao == null || !StringUtils.hasText(identificador)) {
            return "Posicao de patio inexistente.";
        }
        if (posicao.isBloqueada()) {
            return "Posicao de patio bloqueada: " + identificador + ".";
        }
        if (posicao.isInterditada()) {
            return "Posicao de patio interditada: " + identificador + ".";
        }
        if (!posicao.isAreaPermitida()) {
            return "Posicao fora da area permitida para reservas: " + identificador + ".";
        }
        if (posicao.isOcupada()) {
            return "Posicao de patio ocupada no mapa real: " + identificador + ".";
        }
        if (reservadas.contains(identificador.toUpperCase(Locale.ROOT))
                || reservaRepositorio.existsByPosicaoPatioIdIgnoreCaseAndStatusIn(identificador, STATUS_ATIVO)) {
            return "Posicao de patio ja reservada: " + identificador + ".";
        }
        if (!posicao.getTiposCargaPermitidos().isEmpty()
                && item.getTipoCarga() != null
                && posicao.getTiposCargaPermitidos().stream()
                        .noneMatch(tipo -> item.getTipoCarga().name().equalsIgnoreCase(tipo))) {
            return "Tipo de carga " + item.getTipoCarga() + " nao permitido na posicao " + identificador + ".";
        }
        BigDecimal pesoReserva = item.getPesoUnitarioToneladas() != null
                ? item.getPesoUnitarioToneladas()
                : item.getPesoTotalToneladas();
        if (posicao.getPesoMaximoToneladas() != null
                && pesoReserva != null
                && pesoReserva.compareTo(posicao.getPesoMaximoToneladas()) > 0) {
            return "Peso da carga excede o limite da posicao " + identificador + ".";
        }
        if (posicao.getAlturaMaximaMetros() != null && item.getAlturaCargaMetros() == null) {
            return "Altura da carga deve ser informada para reservar a posicao " + identificador + ".";
        }
        if (posicao.getAlturaMaximaMetros() != null
                && item.getAlturaCargaMetros() != null
                && item.getAlturaCargaMetros().compareTo(posicao.getAlturaMaximaMetros()) > 0) {
            return "Altura da carga excede o limite da posicao " + identificador + ".";
        }
        Integer nivelCamada = extrairNivelCamada(posicao.getCamadaOperacional());
        if (posicao.getCamadaMaxima() != null
                && nivelCamada != null
                && nivelCamada > posicao.getCamadaMaxima()) {
            return "Camada " + nivelCamada + " excede a camada maxima da pilha na posicao " + identificador + ".";
        }
        long totalPilha = posicao.getOcupacaoPilha()
                + reservasPorPilha.getOrDefault(chavePilha(posicao), 0L);
        if (posicao.getCapacidadePilha() != null && totalPilha >= posicao.getCapacidadePilha()) {
            return "Capacidade da pilha esgotada para a posicao " + identificador + ".";
        }
        return null;
    }

    private void cancelar(ReservaPosicaoPatioNavio reserva,
                           String motivo,
                           String usuario,
                           ItemOperacaoNavio item) {
        transicionar(reserva, StatusReservaPatioNavio.CANCELADA, motivo, usuario, item);
    }

    private void transicionar(ReservaPosicaoPatioNavio reserva,
                               StatusReservaPatioNavio novoStatus,
                               String motivo,
                               String usuario,
                               ItemOperacaoNavio item) {
        if (reserva.getStatus() != StatusReservaPatioNavio.ATIVA) {
            return;
        }
        StatusReservaPatioNavio anterior = reserva.getStatus();
        reserva.setStatus(novoStatus);
        reserva.setMotivoCancelamento(motivo);
        reservaRepositorio.save(reserva);

        var visita = item != null ? item.getVisitaNavio() : visitaServico.buscarEntidade(reserva.getVisitaNavioId());
        String tipoEvento = switch (novoStatus) {
            case CONSUMIDA -> "RESERVA_PATIO_CONSUMIDA";
            case CANCELADA -> "RESERVA_PATIO_CANCELADA";
            case EXPIRADA -> "RESERVA_PATIO_EXPIRADA";
            default -> "RESERVA_PATIO_ATUALIZADA";
        };
        String descricao = "Reserva " + reserva.getId() + " da posicao " + reserva.getPosicaoPatioId()
                + " alterada para " + novoStatus + (StringUtils.hasText(motivo) ? ": " + motivo : ".");
        visitaServico.registrarEvento(visita, item, tipoEvento, descricao, usuario,
                anterior.name(), novoStatus.name());
    }

    private Integer extrairNivelCamada(String camada) {
        if (!StringUtils.hasText(camada)) {
            return null;
        }
        String digitos = camada.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digitos)) {
            return null;
        }
        try {
            return Integer.valueOf(digitos);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String chavePilha(PosicaoPatioYardDTO posicao) {
        return chavePilha(posicao.getLinha(), posicao.getColuna());
    }

    private String chavePilha(Integer linha, Integer coluna) {
        return linha + ":" + coluna;
    }

    private String normalizarBloco(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }
}
