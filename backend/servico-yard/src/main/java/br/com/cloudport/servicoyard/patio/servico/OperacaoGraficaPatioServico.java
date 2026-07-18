package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.AtualizarRestricaoPilhaDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerMapaDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.EventoMapaTempoRealDto;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.dto.MovimentarConteinerPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.util.List;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperacaoGraficaPatioServico {

    private static final String TOPICO_ATUALIZACOES = "/topico/patio";

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final MapaPatioServico mapaPatioServico;
    private final SimpMessagingTemplate messagingTemplate;

    public OperacaoGraficaPatioServico(ConteinerPatioRepositorio conteinerRepositorio,
                                        PosicaoPatioRepositorio posicaoRepositorio,
                                        MapaPatioServico mapaPatioServico,
                                        SimpMessagingTemplate messagingTemplate) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.mapaPatioServico = mapaPatioServico;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ConteinerMapaDto movimentar(Long conteinerId, MovimentarConteinerPatioDto dto) {
        ConteinerPatio conteiner = conteinerRepositorio.findById(conteinerId)
                .orElseThrow(() -> new IllegalArgumentException("Contêiner não encontrado."));
        PosicaoPatio destino = posicaoRepositorio.findByLinhaAndColunaAndCamadaOperacional(
                        dto.getLinhaDestino(), dto.getColunaDestino(), dto.getCamadaDestino())
                .orElseThrow(() -> new IllegalArgumentException("A posição de destino não existe no modelo do pátio."));

        validarDestino(conteiner, destino);

        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setId(conteiner.getId());
        requisicao.setCodigo(conteiner.getCodigo());
        requisicao.setLinha(destino.getLinha());
        requisicao.setColuna(destino.getColuna());
        requisicao.setCamadaOperacional(destino.getCamadaOperacional());
        requisicao.setStatus(conteiner.getStatus());
        requisicao.setTipoCarga(conteiner.getTipoCarga() == null
                ? conteiner.getCarga() == null ? null : conteiner.getCarga().getCodigo()
                : conteiner.getTipoCarga().name());
        requisicao.setDestino(conteiner.getDestino());
        return mapaPatioServico.registrarOuAtualizarConteiner(requisicao);
    }

    @Transactional
    public void atualizarRestricao(Long posicaoId, AtualizarRestricaoPilhaDto dto) {
        PosicaoPatio referencia = posicaoRepositorio.findById(posicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Posição do pátio não encontrada."));
        List<PosicaoPatio> pilha = posicaoRepositorio.findAll().stream()
                .filter(posicao -> Objects.equals(posicao.getLinha(), referencia.getLinha()))
                .filter(posicao -> Objects.equals(posicao.getColuna(), referencia.getColuna()))
                .filter(posicao -> Objects.equals(posicao.getBloco(), referencia.getBloco()))
                .toList();
        if (pilha.isEmpty()) {
            throw new IllegalArgumentException("A pilha selecionada não possui posições configuradas.");
        }

        pilha.forEach(posicao -> {
            if (dto.getBloqueada() != null) {
                posicao.setBloqueada(dto.getBloqueada());
            }
            if (dto.getInterditada() != null) {
                posicao.setInterditada(dto.getInterditada());
            }
            if (dto.getAreaPermitida() != null) {
                posicao.setAreaPermitida(dto.getAreaPermitida());
            }
            posicao.setNotaOperacional(StringUtils.hasText(dto.getNotaOperacional())
                    ? dto.getNotaOperacional()
                    : null);
        });
        posicaoRepositorio.saveAll(pilha);
        publicarAtualizacao();
    }

    private void validarDestino(ConteinerPatio conteiner, PosicaoPatio destino) {
        if (conteiner.getPosicao() != null && Objects.equals(conteiner.getPosicao().getId(), destino.getId())) {
            throw new IllegalArgumentException("O contêiner já está na posição selecionada.");
        }
        if (destino.isInterditada()) {
            throw new IllegalArgumentException("A posição de destino está interditada.");
        }
        if (destino.isBloqueada() || !destino.isAreaPermitida()) {
            throw new IllegalArgumentException("A posição de destino está bloqueada para movimentação.");
        }
        boolean ocupada = conteinerRepositorio.findAll().stream()
                .filter(outro -> !Objects.equals(outro.getId(), conteiner.getId()))
                .anyMatch(outro -> outro.getPosicao() != null
                        && Objects.equals(outro.getPosicao().getId(), destino.getId()));
        if (ocupada) {
            throw new IllegalArgumentException("A posição de destino já está ocupada.");
        }
    }

    private void publicarAtualizacao() {
        MapaPatioRespostaDto mapa = mapaPatioServico.consultarMapa(
                mapaPatioServico.construirFiltro(List.of(), List.of(), List.of(), List.of(), List.of()));
        messagingTemplate.convertAndSend(TOPICO_ATUALIZACOES,
                new EventoMapaTempoRealDto("RESTRICAO_PILHA_ATUALIZADA", mapa));
    }
}