package br.com.cloudport.serviconaviosiderurgico.servico;

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
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReservaPatioNavioServico {

    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final VisitaNavioServico visitaServico;

    public ReservaPatioNavioServico(
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            ItemOperacaoNavioRepositorio itemRepositorio,
            VisitaNavioServico visitaServico
    ) {
        this.reservaRepositorio = reservaRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.visitaServico = visitaServico;
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

        List<ReservaPatioNavioDTO> reservas = itens.stream()
                .filter(item -> !somentePendentes || reservaAtiva(item) == null)
                .map(item -> reservarItem(item, tipoReserva))
                .map(ReservaPatioNavioDTO::de)
                .toList();

        if (!reservas.isEmpty()) {
            visitaServico.registrarEvento(visita, null, "RESERVAS_PATIO_GERADAS", reservas.size() + " reserva(s) de patio gerada(s) para descarga.", comando == null ? null : comando.usuario(), null, String.valueOf(reservas.size()));
        }
        return reservas;
    }

    @Transactional
    public ReservaPosicaoPatioNavio reservarItem(ItemOperacaoNavio item, TipoReservaPatioNavio tipoReserva) {
        ReservaPosicaoPatioNavio reservaExistente = reservaAtiva(item);
        if (reservaExistente != null) {
            return reservaExistente;
        }
        String posicao = posicaoPlanejada(item);
        validarPosicaoDisponivel(posicao);

        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        reserva.setVisitaNavioId(item.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(item.getId());
        reserva.setPosicaoPatioId(posicao);
        reserva.setTipoReserva(tipoReserva == null ? TipoReservaPatioNavio.TENTATIVA : tipoReserva);
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        preencherCoordenadas(reserva, posicao);
        ReservaPosicaoPatioNavio salva = reservaRepositorio.save(reserva);

        item.setPosicaoPatioPlanejada(posicao);
        item.setDestinoPatio(StringUtils.hasText(item.getDestinoPatio()) ? item.getDestinoPatio() : posicao);
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
        itemRepositorio.save(item);
        return salva;
    }

    private ReservaPosicaoPatioNavio reservaAtiva(ItemOperacaoNavio item) {
        return reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                item.getId(), List.of(StatusReservaPatioNavio.ATIVA))
                .orElse(null);
    }

    private String posicaoPlanejada(ItemOperacaoNavio item) {
        if (StringUtils.hasText(item.getPosicaoPatioPlanejada())) {
            return item.getPosicaoPatioPlanejada().trim().toUpperCase(Locale.ROOT);
        }
        if (StringUtils.hasText(item.getDestinoPatio())) {
            return item.getDestinoPatio().trim().toUpperCase(Locale.ROOT);
        }
        int sequencia = item.getSequenciaOperacional() == null ? item.getId().intValue() : item.getSequenciaOperacional();
        return "V" + item.getVisitaNavio().getId() + "-D-" + sequencia;
    }

    private void validarPosicaoDisponivel(String posicao) {
        if (!StringUtils.hasText(posicao)) {
            throw new IllegalArgumentException("Nao ha posicao de patio disponivel para reserva.");
        }
        String normalizada = posicao.trim().toUpperCase(Locale.ROOT);
        if (normalizada.contains("BLOQUE") || normalizada.contains("OCUP")) {
            throw new IllegalArgumentException("Posicao de patio bloqueada ou ocupada: " + posicao + ".");
        }
        if (reservaRepositorio.existsByPosicaoPatioIdIgnoreCaseAndStatusIn(normalizada, List.of(StatusReservaPatioNavio.ATIVA))) {
            throw new IllegalArgumentException("Posicao de patio ja reservada: " + posicao + ".");
        }
    }

    private void preencherCoordenadas(ReservaPosicaoPatioNavio reserva, String posicao) {
        String[] partes = posicao.split("[-/]");
        if (partes.length > 0) {
            reserva.setBloco(partes[0]);
        }
        if (partes.length > 1) {
            reserva.setLinha(parseInteiro(partes[1]));
        }
        if (partes.length > 2) {
            reserva.setColuna(parseInteiro(partes[2]));
        }
        if (partes.length > 3) {
            reserva.setCamada(partes[3]);
        }
    }

    private Integer parseInteiro(String valor) {
        try {
            return Integer.valueOf(valor.replaceAll("\\D", ""));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
