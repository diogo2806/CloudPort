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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReservaPatioNavioServico {

    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final VisitaNavioServico visitaServico;
    private final PosicaoPatioYardCliente posicaoPatioYardCliente;

    public ReservaPatioNavioServico(
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            ItemOperacaoNavioRepositorio itemRepositorio,
            VisitaNavioServico visitaServico,
            PosicaoPatioYardCliente posicaoPatioYardCliente
    ) {
        this.reservaRepositorio = reservaRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.visitaServico = visitaServico;
        this.posicaoPatioYardCliente = posicaoPatioYardCliente;
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
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(visitaId).stream()
                .filter(item -> item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA)
                .filter(item -> item.getStatus() != StatusItemCarga.OPERADO && item.getStatus() != StatusItemCarga.CANCELADO)
                .sorted(Comparator.comparing(ItemOperacaoNavio::getSequenciaOperacional, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<PosicaoPatioYardDTO> posicoes = posicoesDisponiveisDoYard();
        Set<String> posicoesReservadas = posicoesReservadasAtivas();

        List<ReservaPatioNavioDTO> reservas = itens.stream()
                .filter(item -> !somentePendentes || reservaAtiva(item) == null)
                .map(item -> reservarItem(item, tipoReserva, posicoes, posicoesReservadas))
                .map(ReservaPatioNavioDTO::de)
                .toList();

        if (!reservas.isEmpty()) {
            visitaServico.registrarEvento(visita, null, "RESERVAS_PATIO_GERADAS",
                    reservas.size() + " reserva(s) de patio real gerada(s) para descarga.",
                    comando == null ? null : comando.usuario(), null, String.valueOf(reservas.size()));
        }
        return reservas;
    }

    @Transactional
    public ReservaPosicaoPatioNavio reservarItem(ItemOperacaoNavio item, TipoReservaPatioNavio tipoReserva) {
        return reservarItem(item, tipoReserva, posicoesDisponiveisDoYard(), posicoesReservadasAtivas());
    }

    private ReservaPosicaoPatioNavio reservarItem(ItemOperacaoNavio item,
                                                    TipoReservaPatioNavio tipoReserva,
                                                    List<PosicaoPatioYardDTO> posicoes,
                                                    Set<String> posicoesReservadas) {
        ReservaPosicaoPatioNavio reservaExistente = reservaAtiva(item);
        if (reservaExistente != null) {
            return reservaExistente;
        }

        PosicaoPatioYardDTO posicao = selecionarPosicao(item, posicoes, posicoesReservadas);
        String identificador = posicao.identificador();
        validarPosicaoDisponivel(posicao, identificador, posicoesReservadas);

        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        reserva.setVisitaNavioId(item.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(item.getId());
        reserva.setPosicaoPatioId(identificador);
        reserva.setTipoReserva(tipoReserva == null ? TipoReservaPatioNavio.TENTATIVA : tipoReserva);
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        reserva.setBloco(normalizarBloco(item.getDestinoPatio()));
        reserva.setLinha(posicao.getLinha());
        reserva.setColuna(posicao.getColuna());
        reserva.setCamada(posicao.getCamadaOperacional());
        ReservaPosicaoPatioNavio salva = reservaRepositorio.save(reserva);
        posicoesReservadas.add(identificador.toUpperCase(Locale.ROOT));

        item.setPosicaoPatioPlanejada(identificador);
        item.setDestinoPatio(StringUtils.hasText(item.getDestinoPatio()) ? item.getDestinoPatio() : identificador);
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
        itemRepositorio.save(item);
        return salva;
    }

    private ReservaPosicaoPatioNavio reservaAtiva(ItemOperacaoNavio item) {
        return reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                        item.getId(), List.of(StatusReservaPatioNavio.ATIVA))
                .orElse(null);
    }

    private List<PosicaoPatioYardDTO> posicoesDisponiveisDoYard() {
        List<PosicaoPatioYardDTO> posicoes = posicaoPatioYardCliente.listarPosicoes().stream()
                .filter(Objects::nonNull)
                .filter(posicao -> posicao.getId() != null)
                .filter(posicao -> posicao.getLinha() != null && posicao.getColuna() != null)
                .filter(posicao -> StringUtils.hasText(posicao.getCamadaOperacional()))
                .sorted(Comparator.comparing(PosicaoPatioYardDTO::getLinha)
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
                .map(ReservaPosicaoPatioNavio::getPosicaoPatioId)
                .filter(StringUtils::hasText)
                .map(valor -> valor.trim().toUpperCase(Locale.ROOT))
                .forEach(reservadas::add);
        return reservadas;
    }

    private PosicaoPatioYardDTO selecionarPosicao(ItemOperacaoNavio item,
                                                   List<PosicaoPatioYardDTO> posicoes,
                                                   Set<String> reservadas) {
        String preferida = normalizar(item.getPosicaoPatioPlanejada());
        if (StringUtils.hasText(preferida)) {
            PosicaoPatioYardDTO encontrada = posicoes.stream()
                    .filter(posicao -> correspondePreferencia(posicao, preferida))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A posicao planejada " + preferida + " nao existe no mapa real do patio."));
            validarPosicaoDisponivel(encontrada, encontrada.identificador(), reservadas);
            return encontrada;
        }

        return posicoes.stream()
                .filter(posicao -> !posicao.isOcupada())
                .filter(posicao -> !reservadas.contains(posicao.identificador().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nao ha posicao real livre e sem reserva ativa no patio."));
    }

    private boolean correspondePreferencia(PosicaoPatioYardDTO posicao, String preferida) {
        String porId = normalizar(posicao.identificador());
        String porCoordenada = normalizar(posicao.getLinha() + "-" + posicao.getColuna() + "-" + posicao.getCamadaOperacional());
        return preferida.equals(porId) || preferida.equals(porCoordenada);
    }

    private void validarPosicaoDisponivel(PosicaoPatioYardDTO posicao,
                                           String identificador,
                                           Set<String> reservadas) {
        if (posicao == null || !StringUtils.hasText(identificador)) {
            throw new IllegalArgumentException("Posicao de patio inexistente.");
        }
        if (posicao.isOcupada()) {
            throw new IllegalArgumentException("Posicao de patio ocupada no mapa real: " + identificador + ".");
        }
        if (reservadas.contains(identificador.toUpperCase(Locale.ROOT))
                || reservaRepositorio.existsByPosicaoPatioIdIgnoreCaseAndStatusIn(
                        identificador, List.of(StatusReservaPatioNavio.ATIVA))) {
            throw new IllegalArgumentException("Posicao de patio ja reservada: " + identificador + ".");
        }
    }

    private String normalizarBloco(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }
}
