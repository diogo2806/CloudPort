package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.ConteinerInventarioResposta;
import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.LiberarConteinerRequest;
import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.ReservarConteinerRequest;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaConteinerCargaGeral;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaConteinerCargaGeral.StatusReserva;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario.CategoriaEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario.CondicaoEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario.EstadoUnidade;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario.StatusManutencao;
import br.com.cloudport.servicoyard.inventario.repositorio.ReservaConteinerCargaGeralRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservaConteinerCargaGeralServico {

    private static final Set<EstadoUnidade> ESTADOS_ELEGIVEIS = EnumSet.of(
            EstadoUnidade.ATIVA,
            EstadoUnidade.NO_PATIO,
            EstadoUnidade.DESEMBARCADA);

    private final UnidadeInventarioRepositorio unidadeRepositorio;
    private final ReservaConteinerCargaGeralRepositorio reservaRepositorio;

    public ReservaConteinerCargaGeralServico(
            UnidadeInventarioRepositorio unidadeRepositorio,
            ReservaConteinerCargaGeralRepositorio reservaRepositorio) {
        this.unidadeRepositorio = unidadeRepositorio;
        this.reservaRepositorio = reservaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ConteinerInventarioResposta> listarElegiveis() {
        Set<Long> unidadesReservadas = reservaRepositorio.findAllByStatus(StatusReserva.ATIVA).stream()
                .map(reserva -> reserva.getUnidade().getId())
                .collect(Collectors.toSet());
        return unidadeRepositorio.findAllByOrderByIdentificacaoAsc().stream()
                .filter(unidade -> !unidadesReservadas.contains(unidade.getId()))
                .filter(this::elegivel)
                .map(unidade -> mapear(unidade, null))
                .toList();
    }

    @Transactional
    public ConteinerInventarioResposta reservar(
            String identificacao,
            ReservarConteinerRequest request) {
        ReservaConteinerCargaGeral reservaDaOperacao = reservaRepositorio.findByOperacaoId(request.operacaoId())
                .orElse(null);
        if (reservaDaOperacao != null) {
            if (reservaDaOperacao.getStatus() == StatusReserva.ATIVA
                    && reservaDaOperacao.getUnidade().getIdentificacao().equalsIgnoreCase(identificacao)) {
                return mapear(reservaDaOperacao.getUnidade(), reservaDaOperacao);
            }
            throw conflito("A operação já possui uma reserva de contêiner incompatível.");
        }

        UnidadeInventario unidade = unidadeRepositorio.findComBloqueioByIdentificacaoIgnoreCase(identificacao)
                .orElseThrow(() -> naoEncontrada("Contêiner não encontrado no inventário canônico."));
        ReservaConteinerCargaGeral reservaAtiva = reservaRepositorio
                .findByUnidadeIdAndStatus(unidade.getId(), StatusReserva.ATIVA)
                .orElse(null);
        if (reservaAtiva != null) {
            if (reservaAtiva.getOperacaoId().equals(request.operacaoId())) {
                return mapear(unidade, reservaAtiva);
            }
            throw conflito("Contêiner já está vinculado a outra operação de carga geral.");
        }

        validarElegibilidade(unidade);
        ReservaConteinerCargaGeral reserva = new ReservaConteinerCargaGeral();
        reserva.setUnidade(unidade);
        reserva.setOperacaoId(request.operacaoId());
        reserva.setEstadoAnterior(unidade.getEstado());
        reserva.setUsuarioReserva(request.usuario());
        unidade.setEstado(EstadoUnidade.EM_OPERACAO);
        unidadeRepositorio.save(unidade);
        return mapear(unidade, reservaRepositorio.save(reserva));
    }

    @Transactional
    public ConteinerInventarioResposta liberar(
            UUID operacaoId,
            LiberarConteinerRequest request) {
        ReservaConteinerCargaGeral reserva = reservaRepositorio.findByOperacaoId(operacaoId)
                .orElseThrow(() -> naoEncontrada("Reserva de contêiner da operação não encontrada."));
        UnidadeInventario unidade = unidadeRepositorio.findComBloqueioById(reserva.getUnidade().getId())
                .orElseThrow(() -> naoEncontrada("Contêiner reservado não foi encontrado no inventário."));
        if (reserva.getStatus() == StatusReserva.ATIVA) {
            if (unidade.getEstado() == EstadoUnidade.EM_OPERACAO) {
                unidade.setEstado(reserva.getEstadoAnterior());
                unidadeRepositorio.save(unidade);
            }
            reserva.liberar(request.usuario(), request.motivo(), request.resultado());
            reservaRepositorio.save(reserva);
        }
        return mapear(unidade, reserva);
    }

    private boolean elegivel(UnidadeInventario unidade) {
        return unidade.getCategoria() == CategoriaEquipamento.CONTEINER
                && unidade.getCondicao() == CondicaoEquipamento.OPERACIONAL
                && (unidade.getStatusManutencao() == StatusManutencao.NAO_REQUERIDA
                    || unidade.getStatusManutencao() == StatusManutencao.CONCLUIDA)
                && ESTADOS_ELEGIVEIS.contains(unidade.getEstado())
                && !unidade.possuiHoldAtivo(LocalDateTime.now());
    }

    private void validarElegibilidade(UnidadeInventario unidade) {
        if (unidade.getCategoria() != CategoriaEquipamento.CONTEINER) {
            throw conflito("A unidade informada não é um contêiner canônico.");
        }
        if (unidade.getCondicao() != CondicaoEquipamento.OPERACIONAL) {
            throw conflito("Contêiner não está em condição operacional.");
        }
        if (unidade.getStatusManutencao() != StatusManutencao.NAO_REQUERIDA
                && unidade.getStatusManutencao() != StatusManutencao.CONCLUIDA) {
            throw conflito("Contêiner possui manutenção impeditiva.");
        }
        if (!ESTADOS_ELEGIVEIS.contains(unidade.getEstado())) {
            throw conflito("Estado atual do contêiner não permite operação de stuff ou unstuff.");
        }
        if (unidade.possuiHoldAtivo(LocalDateTime.now())) {
            throw conflito("Contêiner possui hold ativo no inventário canônico.");
        }
    }

    private ConteinerInventarioResposta mapear(
            UnidadeInventario unidade,
            ReservaConteinerCargaGeral reserva) {
        return new ConteinerInventarioResposta(
                unidade.getId(),
                unidade.getIdentificacao(),
                unidade.getEstado().name(),
                unidade.getCondicao().name(),
                unidade.getPosicaoAtual(),
                reserva == null ? null : reserva.getOperacaoId(),
                reserva == null ? null : reserva.getStatus().name());
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
