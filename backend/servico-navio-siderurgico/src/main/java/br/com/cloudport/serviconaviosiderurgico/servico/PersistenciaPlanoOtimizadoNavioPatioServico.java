package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente.PosicaoPatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemPlanoOtimizadoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class PersistenciaPlanoOtimizadoNavioPatioServico {

    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final PosicaoPatioYardCliente posicaoPatioYardCliente;
    private final long duracaoReservaMinutos;

    public PersistenciaPlanoOtimizadoNavioPatioServico(
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            PosicaoPatioYardCliente posicaoPatioYardCliente,
            @Value("${cloudport.integracao.yard.reserva-duracao-minutos:120}") long duracaoReservaMinutos
    ) {
        this.itemRepositorio = itemRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.posicaoPatioYardCliente = posicaoPatioYardCliente;
        this.duracaoReservaMinutos = Math.max(1, duracaoReservaMinutos);
    }

    @Transactional
    public List<ReservaPatioNavioDTO> aplicar(
            Long visitaId,
            String planoId,
            List<ItemPlanoOtimizadoNavioPatioDTO> itensPlano
    ) {
        if (visitaId == null || !StringUtils.hasText(planoId)
                || itensPlano == null || itensPlano.isEmpty()) {
            throw new IllegalArgumentException("Plano, visita e itens devem ser informados para aplicacao local.");
        }

        Map<Long, ItemOperacaoNavio> itensPorId = new LinkedHashMap<>();
        itemRepositorio.findByVisitaNavioId(visitaId).forEach(item -> itensPorId.put(item.getId(), item));
        Map<Long, ReservaPosicaoPatioNavio> reservasAtivas = reservasAtivasPorItem();
        Map<String, PosicaoPatioYardDTO> posicoesPorCoordenada = posicoesPorCoordenada();
        Set<String> destinosDoPlano = new HashSet<>();
        List<AlteracaoLocal> alteracoes = new ArrayList<>();

        for (ItemPlanoOtimizadoNavioPatioDTO itemPlano : itensPlano) {
            ItemOperacaoNavio item = itensPorId.get(itemPlano.itemOperacaoNavioId());
            if (item == null) {
                throw new IllegalStateException("O item " + itemPlano.itemOperacaoNavioId()
                        + " nao pertence a visita do plano " + planoId + ".");
            }
            validarItem(item, itemPlano);
            String chave = chavePosicao(itemPlano.linha(), itemPlano.coluna(), itemPlano.camada());
            if (!destinosDoPlano.add(chave)) {
                throw new IllegalStateException("O plano " + planoId
                        + " atribui mais de um item para a posicao " + chave + ".");
            }
            PosicaoPatioYardDTO posicao = posicoesPorCoordenada.get(chave);
            if (posicao == null) {
                throw new IllegalStateException("A posicao " + chave
                        + " do plano nao existe no mapa real do Yard.");
            }
            ReservaPosicaoPatioNavio reservaAnterior = reservasAtivas.get(item.getId());
            validarPosicao(item, posicao, reservaAnterior, reservasAtivas.values());
            alteracoes.add(new AlteracaoLocal(item, itemPlano, posicao, reservaAnterior));
        }

        LocalDateTime expiraEm = LocalDateTime.now().plusMinutes(duracaoReservaMinutos);
        List<ReservaPatioNavioDTO> reservasAplicadas = new ArrayList<>();
        for (AlteracaoLocal alteracao : alteracoes) {
            ItemOperacaoNavio item = alteracao.item();
            PosicaoPatioYardDTO posicao = alteracao.posicao();
            ReservaPosicaoPatioNavio anterior = alteracao.reservaAnterior();
            ReservaPosicaoPatioNavio reserva;

            if (mesmaPosicao(anterior, posicao)) {
                reserva = anterior;
                reserva.setTipoReserva(TipoReservaPatioNavio.DEFINITIVA);
                reserva.setStatus(StatusReservaPatioNavio.ATIVA);
                reserva.setMotivoCancelamento(null);
                reserva.setExpiraEm(expiraEm);
                reservaRepositorio.save(reserva);
            } else {
                if (anterior != null) {
                    anterior.setStatus(StatusReservaPatioNavio.CANCELADA);
                    anterior.setMotivoCancelamento("Compensada pelo plano otimizado " + planoId + ".");
                    reservaRepositorio.save(anterior);
                }
                reserva = novaReserva(item, posicao, anterior, expiraEm);
                reservaRepositorio.save(reserva);
            }

            item.setPosicaoPatioPlanejada(posicao.identificador());
            item.setDestinoPatio(StringUtils.hasText(posicao.getBloco())
                    ? posicao.getBloco().trim().toUpperCase(Locale.ROOT)
                    : posicao.identificador());
            item.setSequenciaOperacional(alteracao.itemPlano().sequenciaPlano());
            item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ORDEM_GERADA);
            itemRepositorio.save(item);
            reservasAplicadas.add(ReservaPatioNavioDTO.de(reserva));
        }
        return List.copyOf(reservasAplicadas);
    }

    private void validarItem(ItemOperacaoNavio item, ItemPlanoOtimizadoNavioPatioDTO itemPlano) {
        if (!item.getCodigoLote().equalsIgnoreCase(itemPlano.codigoCarga())) {
            throw new IllegalStateException("O codigo do item " + item.getId() + " diverge do plano otimizado.");
        }
        if (!Objects.equals(item.getOrdemTrabalhoPatioId(), itemPlano.ordemTrabalhoPatioId())) {
            throw new IllegalStateException("A ordem real do item " + item.getCodigoLote()
                    + " mudou depois da geracao do plano.");
        }
        if (item.getStatus() == StatusItemCarga.BLOQUEADO
                || item.getStatus() == StatusItemCarga.OPERADO
                || item.getStatus() == StatusItemCarga.CANCELADO) {
            throw new IllegalStateException("O item " + item.getCodigoLote()
                    + " nao pode ser replanejado no status " + item.getStatus() + ".");
        }
    }

    private void validarPosicao(
            ItemOperacaoNavio item,
            PosicaoPatioYardDTO posicao,
            ReservaPosicaoPatioNavio reservaAnterior,
            java.util.Collection<ReservaPosicaoPatioNavio> reservasAtivas
    ) {
        String identificador = posicao.identificador();
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()) {
            throw new IllegalStateException("A posicao " + identificador
                    + " nao esta operacionalmente disponivel.");
        }
        if (posicao.isOcupada()
                && (!StringUtils.hasText(posicao.getCodigoConteiner())
                || !posicao.getCodigoConteiner().equalsIgnoreCase(item.getCodigoLote()))) {
            throw new IllegalStateException("A posicao " + identificador
                    + " esta ocupada por outra carga no mapa real.");
        }
        boolean reservadaPorOutroItem = reservasAtivas.stream()
                .filter(reserva -> reserva.getStatus() == StatusReservaPatioNavio.ATIVA)
                .filter(reserva -> !Objects.equals(reserva.getItemOperacaoNavioId(), item.getId()))
                .anyMatch(reserva -> Objects.equals(
                        normalizar(reserva.getPosicaoPatioId()),
                        normalizar(identificador)));
        if (reservadaPorOutroItem) {
            throw new IllegalStateException("A posicao " + identificador
                    + " possui reserva ativa de outro item.");
        }
        if (!posicao.getTiposCargaPermitidos().isEmpty()
                && posicao.getTiposCargaPermitidos().stream()
                .noneMatch(tipo -> item.getTipoCarga().name().equalsIgnoreCase(tipo))) {
            throw new IllegalStateException("O tipo de carga " + item.getTipoCarga()
                    + " nao e permitido na posicao " + identificador + ".");
        }
        BigDecimal peso = item.getPesoUnitarioToneladas() != null
                ? item.getPesoUnitarioToneladas()
                : item.getPesoTotalToneladas();
        if (posicao.getPesoMaximoToneladas() != null
                && peso != null
                && peso.compareTo(posicao.getPesoMaximoToneladas()) > 0) {
            throw new IllegalStateException("O peso da carga excede o limite da posicao "
                    + identificador + ".");
        }
        if (posicao.getAlturaMaximaMetros() != null
                && (item.getAlturaCargaMetros() == null
                || item.getAlturaCargaMetros().compareTo(posicao.getAlturaMaximaMetros()) > 0)) {
            throw new IllegalStateException("A altura da carga e incompativel com a posicao "
                    + identificador + ".");
        }
        if (reservaAnterior == null) {
            throw new IllegalStateException("O item " + item.getCodigoLote()
                    + " nao possui reserva real ativa para ser versionada pelo plano.");
        }
    }

    private Map<Long, ReservaPosicaoPatioNavio> reservasAtivasPorItem() {
        Map<Long, ReservaPosicaoPatioNavio> resultado = new HashMap<>();
        reservaRepositorio.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReservaPatioNavio.ATIVA)
                .filter(reserva -> reserva.getExpiraEm() == null
                        || reserva.getExpiraEm().isAfter(LocalDateTime.now()))
                .forEach(reserva -> resultado.put(reserva.getItemOperacaoNavioId(), reserva));
        return resultado;
    }

    private Map<String, PosicaoPatioYardDTO> posicoesPorCoordenada() {
        Map<String, PosicaoPatioYardDTO> resultado = new HashMap<>();
        posicaoPatioYardCliente.listarPosicoes().stream()
                .filter(Objects::nonNull)
                .filter(posicao -> posicao.getId() != null
                        && posicao.getLinha() != null
                        && posicao.getColuna() != null
                        && StringUtils.hasText(posicao.getCamadaOperacional()))
                .forEach(posicao -> resultado.put(
                        chavePosicao(
                                posicao.getLinha(),
                                posicao.getColuna(),
                                posicao.getCamadaOperacional()),
                        posicao));
        if (resultado.isEmpty()) {
            throw new IllegalStateException("O mapa real do Yard nao retornou posicoes validas.");
        }
        return resultado;
    }

    private ReservaPosicaoPatioNavio novaReserva(
            ItemOperacaoNavio item,
            PosicaoPatioYardDTO posicao,
            ReservaPosicaoPatioNavio anterior,
            LocalDateTime expiraEm
    ) {
        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        reserva.setVisitaNavioId(item.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(item.getId());
        reserva.setPosicaoPatioId(posicao.identificador());
        reserva.setBloco(StringUtils.hasText(posicao.getBloco())
                ? posicao.getBloco().trim().toUpperCase(Locale.ROOT)
                : null);
        reserva.setLinha(posicao.getLinha());
        reserva.setColuna(posicao.getColuna());
        reserva.setCamada(posicao.getCamadaOperacional());
        reserva.setTipoReserva(TipoReservaPatioNavio.DEFINITIVA);
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        reserva.setExpiraEm(expiraEm);
        reserva.setReservaAnteriorId(anterior == null ? null : anterior.getId());
        return reserva;
    }

    private boolean mesmaPosicao(ReservaPosicaoPatioNavio reserva, PosicaoPatioYardDTO posicao) {
        return reserva != null
                && Objects.equals(reserva.getLinha(), posicao.getLinha())
                && Objects.equals(reserva.getColuna(), posicao.getColuna())
                && Objects.equals(normalizar(reserva.getCamada()), normalizar(posicao.getCamadaOperacional()));
    }

    private String chavePosicao(Integer linha, Integer coluna, String camada) {
        return linha + "-" + coluna + "-" + normalizar(camada);
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }

    private record AlteracaoLocal(
            ItemOperacaoNavio item,
            ItemPlanoOtimizadoNavioPatioDTO itemPlano,
            PosicaoPatioYardDTO posicao,
            ReservaPosicaoPatioNavio reservaAnterior
    ) {
    }
}
