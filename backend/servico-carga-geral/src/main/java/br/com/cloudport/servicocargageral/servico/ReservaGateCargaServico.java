package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.ConfirmacaoReservaGateCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusReservaGate;
import br.com.cloudport.servicocargageral.dominio.ReservaGateCarga;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ConfirmacaoGateResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ConfirmarReservaGateRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.CriarReservaGateRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.LiberarReservaGateRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ReservaGateResposta;
import br.com.cloudport.servicocargageral.repositorio.ConfirmacaoReservaGateCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ReservaGateCargaRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservaGateCargaServico {

    private static final EnumSet<StatusReservaGate> PENDENTES = EnumSet.of(
            StatusReservaGate.RESERVADA, StatusReservaGate.PARCIAL);

    private final ReservaGateCargaRepositorio reservaRepositorio;
    private final ConfirmacaoReservaGateCargaRepositorio confirmacaoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public ReservaGateCargaServico(
            ReservaGateCargaRepositorio reservaRepositorio,
            ConfirmacaoReservaGateCargaRepositorio confirmacaoRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.reservaRepositorio = reservaRepositorio;
        this.confirmacaoRepositorio = confirmacaoRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ReservaGateResposta> listar() {
        return reservaRepositorio.findAllByOrderByCriadoEmDesc().stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional
    public ReservaGateResposta reservar(CriarReservaGateRequest request) {
        ReservaGateCarga existente = reservaRepositorio.findByTransacaoIdIgnoreCase(request.transacaoId()).orElse(null);
        if (existente != null) {
            validarRepeticao(existente, request);
            return mapear(existente);
        }
        if (request.janelaFim() != null && request.janelaFim().isBefore(OffsetDateTime.now())) {
            throw conflito("A janela do Gate já foi encerrada.");
        }
        LoteCarga lote = buscarLote(request.loteId());
        validarReserva(lote, request.retirada(), request.quantidade(), request.volumeM3(), request.pesoKg());
        ReservaGateCarga reserva = new ReservaGateCarga();
        reserva.setTransacaoId(request.transacaoId());
        reserva.setRetirada(request.retirada());
        reserva.setLote(lote);
        reserva.setBlNumero(request.blNumero());
        reserva.setDeliveryOrder(request.deliveryOrder());
        reserva.setAppointmentId(request.appointmentId());
        reserva.setTruckVisitId(request.truckVisitId());
        reserva.setVeiculoId(request.veiculoId());
        reserva.setJanelaInicio(request.janelaInicio());
        reserva.setJanelaFim(request.janelaFim());
        reserva.setQuantidadeReservada(request.quantidade());
        reserva.setVolumeReservadoM3(request.volumeM3());
        reserva.setPesoReservadoKg(request.pesoKg());
        reserva.setUsuario(request.usuario());
        return mapear(reservaRepositorio.save(reserva));
    }

    @Transactional
    public ReservaGateResposta confirmar(UUID id, ConfirmarReservaGateRequest request) {
        ReservaGateCarga reserva = reservaRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Reserva do Gate não encontrada."));
        return confirmarInterno(reserva, request);
    }

    @Transactional
    public ReservaGateResposta confirmarPorTransacao(String transacaoId, ConfirmarReservaGateRequest request) {
        ReservaGateCarga reserva = reservaRepositorio.findComBloqueioByTransacaoIdIgnoreCase(transacaoId)
                .orElseThrow(() -> naoEncontrado("Reserva do Gate não encontrada."));
        return confirmarInterno(reserva, request);
    }

    @Transactional
    public ReservaGateResposta liberar(UUID id, LiberarReservaGateRequest request) {
        ReservaGateCarga reserva = reservaRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Reserva do Gate não encontrada."));
        executar(() -> reserva.liberar(request.motivo()));
        return mapear(reservaRepositorio.save(reserva));
    }

    private ReservaGateResposta confirmarInterno(ReservaGateCarga reserva, ConfirmarReservaGateRequest request) {
        ConfirmacaoReservaGateCarga existente = confirmacaoRepositorio
                .findByConfirmacaoIdIgnoreCase(request.confirmacaoId()).orElse(null);
        if (existente != null) {
            if (!existente.getReserva().getId().equals(reserva.getId())
                    || existente.getQuantidade().compareTo(request.quantidade()) != 0
                    || existente.getVolumeM3().compareTo(request.volumeM3()) != 0
                    || existente.getPesoKg().compareTo(request.pesoKg()) != 0) {
                throw conflito("O confirmacaoId já foi utilizado com dados diferentes.");
            }
            return mapear(reserva);
        }
        LoteCarga lote = buscarLote(reserva.getLote().getId());
        if (reserva.isRetirada()) {
            validarDisponibilidade(lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        } else {
            validarCapacidade(lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        }
        executar(() -> reserva.confirmar(request.confirmacaoId(), request.quantidade(), request.volumeM3(),
                request.pesoKg(), request.usuario(), request.correlationId()));
        if (reserva.isRetirada()) {
            executar(() -> lote.retirarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg()));
        } else {
            lote.adicionarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
        }
        lote.atualizarLocalizacao(lote.getArmazemId(), lote.getPosicaoArmazenagem(), reserva.getVeiculoId(),
                lote.getVisitaNavioId(), lote.getClienteId());
        lote.registrarMovimentacao(criarMovimentacao(reserva, request));
        loteRepositorio.save(lote);
        return mapear(reservaRepositorio.save(reserva));
    }

    private void validarRepeticao(ReservaGateCarga reserva, CriarReservaGateRequest request) {
        boolean igual = reserva.isRetirada() == request.retirada()
                && reserva.getLote().getId().equals(request.loteId())
                && reserva.getQuantidadeReservada().compareTo(request.quantidade()) == 0
                && reserva.getVolumeReservadoM3().compareTo(request.volumeM3()) == 0
                && reserva.getPesoReservadoKg().compareTo(request.pesoKg()) == 0;
        if (!igual) {
            throw conflito("A transação do Gate já existe com payload diferente.");
        }
    }

    private void validarReserva(LoteCarga lote, boolean retirada, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        List<ReservaGateCarga> reservas = reservaRepositorio.findByLoteIdAndStatusIn(lote.getId(), PENDENTES).stream()
                .filter(item -> item.isRetirada() == retirada).collect(Collectors.toList());
        BigDecimal quantidadePendente = reservas.stream().map(ReservaGateCarga::getQuantidadePendente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal volumePendente = reservas.stream().map(ReservaGateCarga::getVolumePendenteM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pesoPendente = reservas.stream().map(ReservaGateCarga::getPesoPendenteKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (retirada) {
            validarDisponibilidade(lote, quantidade.add(quantidadePendente), volume.add(volumePendente),
                    peso.add(pesoPendente));
        } else {
            validarCapacidade(lote, quantidade.add(quantidadePendente), volume.add(volumePendente),
                    peso.add(pesoPendente));
        }
    }

    private void validarDisponibilidade(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadeSaldo()) > 0
                || volume.compareTo(lote.getVolumeSaldoM3()) > 0
                || peso.compareTo(lote.getPesoSaldoKg()) > 0) {
            throw conflito("Saldo insuficiente para a retirada.");
        }
    }

    private void validarCapacidade(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadePrevista().subtract(lote.getQuantidadeSaldo())) > 0
                || volume.compareTo(lote.getVolumePrevistoM3().subtract(lote.getVolumeSaldoM3())) > 0
                || peso.compareTo(lote.getPesoPrevistoKg().subtract(lote.getPesoSaldoKg())) > 0) {
            throw conflito("Capacidade insuficiente para a entrega.");
        }
    }

    private MovimentacaoCarga criarMovimentacao(ReservaGateCarga reserva, ConfirmarReservaGateRequest request) {
        MovimentacaoCarga movimento = new MovimentacaoCarga();
        movimento.setTipo(reserva.isRetirada() ? TipoMovimentacaoCarga.ENTREGA : TipoMovimentacaoCarga.RECEBIMENTO);
        movimento.setQuantidade(request.quantidade());
        movimento.setVolumeM3(request.volumeM3());
        movimento.setPesoKg(request.pesoKg());
        movimento.setOrigemTipo(reserva.isRetirada() ? "CARGO_LOT" : "CAMINHAO");
        movimento.setOrigemId(reserva.isRetirada() ? reserva.getLote().getId().toString() : reserva.getVeiculoId());
        movimento.setDestinoTipo(reserva.isRetirada() ? "CAMINHAO" : "CARGO_LOT");
        movimento.setDestinoId(reserva.isRetirada() ? reserva.getVeiculoId() : reserva.getLote().getId().toString());
        movimento.setArmazemId(reserva.getLote().getArmazemId());
        movimento.setVeiculoId(reserva.getVeiculoId());
        movimento.setClienteId(reserva.getLote().getClienteId());
        movimento.setUsuario(request.usuario());
        movimento.setCorrelationId(request.correlationId());
        movimento.setObservacao("Gate " + reserva.getTransacaoId() + ", BL " + reserva.getBlNumero() + ".");
        return movimento;
    }

    private LoteCarga buscarLote(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Cargo lot não encontrado."));
    }

    private ReservaGateResposta mapear(ReservaGateCarga reserva) {
        return new ReservaGateResposta(reserva.getId(), reserva.getTransacaoId(), reserva.isRetirada(),
                reserva.getStatus(), reserva.getLote().getId(), reserva.getLote().getCodigo(), reserva.getBlNumero(),
                reserva.getDeliveryOrder(), reserva.getAppointmentId(), reserva.getTruckVisitId(), reserva.getVeiculoId(),
                reserva.getQuantidadeReservada(), reserva.getQuantidadeConfirmada(), reserva.getQuantidadePendente(),
                reserva.getVolumeReservadoM3(), reserva.getVolumeConfirmadoM3(), reserva.getVolumePendenteM3(),
                reserva.getPesoReservadoKg(), reserva.getPesoConfirmadoKg(), reserva.getPesoPendenteKg(),
                reserva.getCriadoEm(), reserva.getConfirmacoes().stream().map(this::mapearConfirmacao)
                        .collect(Collectors.toList()));
    }

    private ConfirmacaoGateResposta mapearConfirmacao(ConfirmacaoReservaGateCarga confirmacao) {
        return new ConfirmacaoGateResposta(confirmacao.getId(), confirmacao.getConfirmacaoId(),
                confirmacao.getQuantidade(), confirmacao.getVolumeM3(), confirmacao.getPesoKg(),
                confirmacao.getUsuario(), confirmacao.getCorrelationId(), confirmacao.getConfirmadoEm());
    }

    private void executar(Runnable comando) {
        try {
            comando.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrado(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
