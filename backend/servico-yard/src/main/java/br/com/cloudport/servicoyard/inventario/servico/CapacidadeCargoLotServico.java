package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ComandoCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ConfigurarCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ReservaCapacidadeResposta;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ReservarCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.SaldoPosicaoResposta;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.StatusReservaCapacidade;
import br.com.cloudport.servicoyard.inventario.modelo.CapacidadePosicaoCargoLot;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaCapacidadeCargoLot;
import br.com.cloudport.servicoyard.inventario.modelo.SaldoPosicaoCargoLot;
import br.com.cloudport.servicoyard.inventario.repositorio.CapacidadePosicaoCargoLotRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.ReservaCapacidadeCargoLotRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.SaldoPosicaoCargoLotRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CapacidadeCargoLotServico {

    private final CapacidadePosicaoCargoLotRepositorio capacidadeRepositorio;
    private final ReservaCapacidadeCargoLotRepositorio reservaRepositorio;
    private final SaldoPosicaoCargoLotRepositorio saldoRepositorio;

    public CapacidadeCargoLotServico(
            CapacidadePosicaoCargoLotRepositorio capacidadeRepositorio,
            ReservaCapacidadeCargoLotRepositorio reservaRepositorio,
            SaldoPosicaoCargoLotRepositorio saldoRepositorio) {
        this.capacidadeRepositorio = capacidadeRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.saldoRepositorio = saldoRepositorio;
    }

    @Transactional
    public void configurar(String posicao, ConfigurarCapacidadeRequest request) {
        CapacidadePosicaoCargoLot capacidade = capacidadeRepositorio.findComBloqueioByPosicaoIgnoreCase(posicao)
                .orElseGet(CapacidadePosicaoCargoLot::new);
        capacidade.setPosicao(posicao);
        capacidade.setCapacidadeQuantidade(request.capacidadeQuantidade());
        capacidade.setCapacidadeVolumeM3(request.capacidadeVolumeM3());
        capacidade.setCapacidadePesoKg(request.capacidadePesoKg());
        capacidade.setRestricoes(request.restricoes());
        capacidade.setAtiva(request.ativa());
        capacidadeRepositorio.save(capacidade);
    }

    @Transactional
    public ReservaCapacidadeResposta reservar(String posicao, ReservarCapacidadeRequest request) {
        ReservaCapacidadeCargoLot existente = reservaRepositorio.findByCommandId(request.commandId()).orElse(null);
        if (existente != null) {
            return mapear(existente);
        }
        CapacidadePosicaoCargoLot capacidade = capacidadeRepositorio.findComBloqueioByPosicaoIgnoreCase(posicao)
                .orElseThrow(() -> naoEncontrada("Capacidade da posição de cargo lot não configurada."));
        if (!capacidade.isAtiva()) {
            throw conflito("Posição de cargo lot está inativa.");
        }
        BigDecimal quantidadeOcupada = ocupadoQuantidade(capacidade.getId());
        BigDecimal volumeOcupado = ocupadoVolume(capacidade.getId());
        BigDecimal pesoOcupado = ocupadoPeso(capacidade.getId());
        if (quantidadeOcupada.add(request.quantidade()).compareTo(capacidade.getCapacidadeQuantidade()) > 0
                || volumeOcupado.add(request.volumeM3()).compareTo(capacidade.getCapacidadeVolumeM3()) > 0
                || pesoOcupado.add(request.pesoKg()).compareTo(capacidade.getCapacidadePesoKg()) > 0) {
            throw conflito("Capacidade insuficiente na posição " + capacidade.getPosicao() + ".");
        }
        ReservaCapacidadeCargoLot reserva = new ReservaCapacidadeCargoLot();
        reserva.setCommandId(request.commandId());
        reserva.setCapacidade(capacidade);
        reserva.setLoteId(request.loteId());
        reserva.setQuantidade(request.quantidade());
        reserva.setVolumeM3(request.volumeM3());
        reserva.setPesoKg(request.pesoKg());
        reserva.setUsuarioReserva(request.usuario());
        return mapear(reservaRepositorio.save(reserva));
    }

    @Transactional
    public ReservaCapacidadeResposta confirmar(UUID reservaId, ComandoCapacidadeRequest request) {
        ReservaCapacidadeCargoLot reserva = buscar(reservaId);
        if (reserva.getStatus() == StatusReservaCapacidade.CONFIRMADA) {
            return mapear(reserva);
        }

        String destino = reserva.getCapacidade().getPosicao();
        String origem = normalizar(request.posicaoOrigem());
        CapacidadePosicaoCargoLot capacidadeDestino = capacidadeRepositorio
                .findComBloqueioByPosicaoIgnoreCase(destino)
                .orElseThrow(() -> naoEncontrada("Capacidade da posição de destino não encontrada."));

        if (StringUtils.hasText(origem) && !origem.equalsIgnoreCase(destino)) {
            CapacidadePosicaoCargoLot capacidadeOrigem = capacidadeRepositorio
                    .findComBloqueioByPosicaoIgnoreCase(origem)
                    .orElseThrow(() -> naoEncontrada("Capacidade da posição de origem não encontrada."));
            SaldoPosicaoCargoLot saldoOrigem = saldoRepositorio
                    .findComBloqueioByCapacidade_IdAndLoteId(capacidadeOrigem.getId(), reserva.getLoteId())
                    .orElseThrow(() -> conflito("Cargo lot não possui saldo na posição de origem informada."));
            executar(() -> saldoOrigem.debitar(
                    reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg()));
            saldoRepositorio.save(saldoOrigem);
        }

        if (!StringUtils.hasText(origem) || !origem.equalsIgnoreCase(destino)) {
            SaldoPosicaoCargoLot saldoDestino = saldoRepositorio
                    .findComBloqueioByCapacidade_IdAndLoteId(capacidadeDestino.getId(), reserva.getLoteId())
                    .orElseGet(() -> novoSaldo(capacidadeDestino, reserva.getLoteId()));
            saldoDestino.creditar(reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg());
            saldoRepositorio.save(saldoDestino);
        }

        executar(() -> reserva.confirmar(request.usuario(), request.motivo()));
        return mapear(reservaRepositorio.save(reserva));
    }

    @Transactional
    public ReservaCapacidadeResposta cancelar(UUID reservaId, ComandoCapacidadeRequest request) {
        ReservaCapacidadeCargoLot reserva = buscar(reservaId);
        executar(() -> reserva.cancelar(request.usuario(), request.motivo()));
        return mapear(reservaRepositorio.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<SaldoPosicaoResposta> listarSaldos(String posicao) {
        CapacidadePosicaoCargoLot capacidade = capacidadeRepositorio.findByPosicaoIgnoreCase(posicao)
                .orElseThrow(() -> naoEncontrada("Capacidade da posição de cargo lot não configurada."));
        return saldoRepositorio.findByCapacidade_IdOrderByAtualizadoEmDesc(capacidade.getId()).stream()
                .filter(saldo -> !saldo.estaZerado())
                .map(saldo -> new SaldoPosicaoResposta(
                        saldo.getLoteId(),
                        capacidade.getPosicao(),
                        saldo.getQuantidade(),
                        saldo.getVolumeM3(),
                        saldo.getPesoKg()))
                .toList();
    }

    private BigDecimal ocupadoQuantidade(UUID capacidadeId) {
        return saldoRepositorio.somarQuantidade(capacidadeId)
                .add(reservaRepositorio.somarQuantidade(capacidadeId, StatusReservaCapacidade.RESERVADA));
    }

    private BigDecimal ocupadoVolume(UUID capacidadeId) {
        return saldoRepositorio.somarVolume(capacidadeId)
                .add(reservaRepositorio.somarVolume(capacidadeId, StatusReservaCapacidade.RESERVADA));
    }

    private BigDecimal ocupadoPeso(UUID capacidadeId) {
        return saldoRepositorio.somarPeso(capacidadeId)
                .add(reservaRepositorio.somarPeso(capacidadeId, StatusReservaCapacidade.RESERVADA));
    }

    private ReservaCapacidadeCargoLot buscar(UUID id) {
        return reservaRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Reserva de capacidade não encontrada."));
    }

    private SaldoPosicaoCargoLot novoSaldo(CapacidadePosicaoCargoLot capacidade, UUID loteId) {
        SaldoPosicaoCargoLot saldo = new SaldoPosicaoCargoLot();
        saldo.setCapacidade(capacidade);
        saldo.setLoteId(loteId);
        return saldo;
    }

    private ReservaCapacidadeResposta mapear(ReservaCapacidadeCargoLot reserva) {
        return new ReservaCapacidadeResposta(
                reserva.getId(),
                reserva.getCommandId(),
                reserva.getLoteId(),
                reserva.getCapacidade().getPosicao(),
                reserva.getQuantidade(),
                reserva.getVolumeM3(),
                reserva.getPesoKg(),
                reserva.getStatus(),
                reserva.getCapacidade().getRestricoes());
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase() : null;
    }

    private void executar(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
