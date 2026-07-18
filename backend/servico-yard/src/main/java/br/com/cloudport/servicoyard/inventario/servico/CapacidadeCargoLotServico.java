package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ComandoCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ConfigurarCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ReservaCapacidadeResposta;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ReservarCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.StatusReservaCapacidade;
import br.com.cloudport.servicoyard.inventario.modelo.CapacidadePosicaoCargoLot;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaCapacidadeCargoLot;
import br.com.cloudport.servicoyard.inventario.repositorio.CapacidadePosicaoCargoLotRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.ReservaCapacidadeCargoLotRepositorio;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CapacidadeCargoLotServico {

    private final CapacidadePosicaoCargoLotRepositorio capacidadeRepositorio;
    private final ReservaCapacidadeCargoLotRepositorio reservaRepositorio;

    public CapacidadeCargoLotServico(
            CapacidadePosicaoCargoLotRepositorio capacidadeRepositorio,
            ReservaCapacidadeCargoLotRepositorio reservaRepositorio) {
        this.capacidadeRepositorio = capacidadeRepositorio;
        this.reservaRepositorio = reservaRepositorio;
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
        executar(() -> reserva.confirmar(request.usuario(), request.motivo()));
        return mapear(reservaRepositorio.save(reserva));
    }

    @Transactional
    public ReservaCapacidadeResposta cancelar(UUID reservaId, ComandoCapacidadeRequest request) {
        ReservaCapacidadeCargoLot reserva = buscar(reservaId);
        executar(() -> reserva.cancelar(request.usuario(), request.motivo()));
        return mapear(reservaRepositorio.save(reserva));
    }

    private BigDecimal ocupadoQuantidade(UUID capacidadeId) {
        return reservaRepositorio.somarQuantidade(capacidadeId, StatusReservaCapacidade.RESERVADA)
                .add(reservaRepositorio.somarQuantidade(capacidadeId, StatusReservaCapacidade.CONFIRMADA));
    }

    private BigDecimal ocupadoVolume(UUID capacidadeId) {
        return reservaRepositorio.somarVolume(capacidadeId, StatusReservaCapacidade.RESERVADA)
                .add(reservaRepositorio.somarVolume(capacidadeId, StatusReservaCapacidade.CONFIRMADA));
    }

    private BigDecimal ocupadoPeso(UUID capacidadeId) {
        return reservaRepositorio.somarPeso(capacidadeId, StatusReservaCapacidade.RESERVADA)
                .add(reservaRepositorio.somarPeso(capacidadeId, StatusReservaCapacidade.CONFIRMADA));
    }

    private ReservaCapacidadeCargoLot buscar(UUID id) {
        return reservaRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Reserva de capacidade não encontrada."));
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
