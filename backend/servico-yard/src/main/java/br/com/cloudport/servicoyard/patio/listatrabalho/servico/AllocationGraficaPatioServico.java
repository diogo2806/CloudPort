package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ReplanejarAllocationPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AllocationGraficaPatioServico {

    private static final List<StatusOrdemTrabalhoPatio> STATUS_REPLANEJAVEIS = List.of(
            StatusOrdemTrabalhoPatio.PENDENTE,
            StatusOrdemTrabalhoPatio.BLOQUEADA,
            StatusOrdemTrabalhoPatio.SUSPENSA);

    private static final List<StatusOrdemTrabalhoPatio> STATUS_ATIVOS = List.of(
            StatusOrdemTrabalhoPatio.PENDENTE,
            StatusOrdemTrabalhoPatio.EM_EXECUCAO,
            StatusOrdemTrabalhoPatio.BLOQUEADA,
            StatusOrdemTrabalhoPatio.SUSPENSA);

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;

    public AllocationGraficaPatioServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                          PosicaoPatioRepositorio posicaoRepositorio,
                                          ConteinerPatioRepositorio conteinerRepositorio) {
        this.ordemRepositorio = ordemRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto replanejar(Long ordemId, ReplanejarAllocationPatioDto dto) {
        OrdemTrabalhoPatio ordem = ordemRepositorio.findByIdAndStatusOrdemIn(ordemId, STATUS_REPLANEJAVEIS)
                .orElseThrow(() -> new IllegalArgumentException(
                        "A work instruction não existe ou não está em um estado que permita replanejamento."));
        PosicaoPatio destino = posicaoRepositorio.findByLinhaAndColunaAndCamadaOperacional(
                        dto.getLinhaDestino(), dto.getColunaDestino(), dto.getCamadaDestino())
                .orElseThrow(() -> new IllegalArgumentException("A posição de destino não existe no modelo do pátio."));

        validarDestino(ordem, destino);
        ordem.setLinhaDestino(destino.getLinha());
        ordem.setColunaDestino(destino.getColuna());
        ordem.setCamadaDestino(destino.getCamadaOperacional());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return OrdemTrabalhoPatioRespostaDto.deEntidade(ordemRepositorio.save(ordem));
    }

    private void validarDestino(OrdemTrabalhoPatio ordem, PosicaoPatio destino) {
        boolean mesmoDestino = Objects.equals(ordem.getLinhaDestino(), destino.getLinha())
                && Objects.equals(ordem.getColunaDestino(), destino.getColuna())
                && ordem.getCamadaDestino().equalsIgnoreCase(destino.getCamadaOperacional());
        if (mesmoDestino) {
            throw new IllegalArgumentException("Selecione uma posição diferente do destino atual da allocation.");
        }
        if (destino.isInterditada()) {
            throw new IllegalArgumentException("A posição escolhida para a allocation está interditada.");
        }
        if (destino.isBloqueada() || !destino.isAreaPermitida()) {
            throw new IllegalArgumentException("A posição escolhida para a allocation está bloqueada.");
        }
        boolean ocupada = conteinerRepositorio.findAll().stream()
                .map(ConteinerPatio::getPosicao)
                .filter(Objects::nonNull)
                .anyMatch(posicao -> Objects.equals(posicao.getId(), destino.getId()));
        if (ocupada) {
            throw new IllegalArgumentException("A posição escolhida para a allocation está ocupada.");
        }
        boolean reservada = ordemRepositorio.findByStatusOrdemInOrderByCriadoEmAsc(STATUS_ATIVOS).stream()
                .filter(outra -> !Objects.equals(outra.getId(), ordem.getId()))
                .anyMatch(outra -> Objects.equals(outra.getLinhaDestino(), destino.getLinha())
                        && Objects.equals(outra.getColunaDestino(), destino.getColuna())
                        && outra.getCamadaDestino().equalsIgnoreCase(destino.getCamadaOperacional()));
        if (reservada) {
            throw new IllegalArgumentException("A posição escolhida já está reservada por outra work instruction.");
        }
    }
}