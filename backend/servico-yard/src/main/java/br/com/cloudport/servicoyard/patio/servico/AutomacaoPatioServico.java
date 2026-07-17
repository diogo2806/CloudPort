package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.RespostaAutoplanejamentoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AutomacaoPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final PosicaoPatioRepositorio posicaoPatioRepositorio;

    public AutomacaoPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                                 EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                                 PosicaoPatioRepositorio posicaoPatioRepositorio) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.posicaoPatioRepositorio = posicaoPatioRepositorio;
    }

    @Transactional
    public RespostaAutoplanejamentoDto executarAutoplanejamento() {
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAll();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAll();
        List<PosicaoPatio> posicoes = posicaoPatioRepositorio.findAll();
        Set<Long> posicoesOcupadas = conteineres.stream()
                .filter(this::ocupaPosicaoAtiva)
                .map(ConteinerPatio::getPosicao)
                .map(PosicaoPatio::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));

        List<ConteinerPatio> conteineresPlanejados = new ArrayList<>();
        List<String> containersPlanificados = new ArrayList<>();
        List<String> containersException = new ArrayList<>();

        for (ConteinerPatio conteiner : conteineres) {
            if (!ehCandidatoAAutoplanejamento(conteiner)) {
                continue;
            }

            Optional<PosicaoPatio> melhorPosicao = encontrarMelhorPosicao(
                    conteiner, posicoes, equipamentos, conteineres, posicoesOcupadas);
            if (melhorPosicao.isEmpty()) {
                containersException.add(conteiner.getCodigo() + " (sem posição disponível)");
                continue;
            }

            PosicaoPatio posicaoSelecionada = melhorPosicao.get();
            if (posicaoSelecionada.getId() == null || !posicoesOcupadas.add(posicaoSelecionada.getId())) {
                throw new IllegalStateException("A posição selecionada não pôde ser reservada no lote de autoplanejamento.");
            }
            conteiner.setPosicao(posicaoSelecionada);
            conteineresPlanejados.add(conteiner);
            containersPlanificados.add(conteiner.getCodigo());
        }

        if (!conteineresPlanejados.isEmpty()) {
            conteinerPatioRepositorio.saveAll(conteineresPlanejados);
            conteinerPatioRepositorio.flush();
        }

        return new RespostaAutoplanejamentoDto(
                conteineres.size(),
                containersPlanificados.size(),
                containersException.size(),
                containersPlanificados,
                containersException
        );
    }

    private boolean ehCandidatoAAutoplanejamento(ConteinerPatio conteiner) {
        if (conteiner.getPosicao() != null || conteiner.getCarga() == null
                || !StringUtils.hasText(conteiner.getCarga().getDescricao())) {
            return false;
        }

        String descricao = conteiner.getCarga().getDescricao().toUpperCase(Locale.ROOT);
        return !descricao.contains("PERIGOSA") && !descricao.contains("IMO");
    }

    private Optional<PosicaoPatio> encontrarMelhorPosicao(ConteinerPatio conteiner,
                                                           List<PosicaoPatio> posicoes,
                                                           List<EquipamentoPatio> equipamentos,
                                                           List<ConteinerPatio> conteineres,
                                                           Set<Long> posicoesOcupadas) {
        return posicoes.stream()
                .filter(posicao -> posicao.getId() != null)
                .filter(posicao -> !estaOcupada(posicao, posicoesOcupadas))
                .filter(posicao -> ehCompativel(conteiner, posicao, equipamentos))
                .sorted(Comparator
                        .comparingInt((PosicaoPatio posicao) -> calcularRehandles(posicao, conteineres))
                        .thenComparing(PosicaoPatio::getLinha)
                        .thenComparing(PosicaoPatio::getColuna)
                        .thenComparing(PosicaoPatio::getId))
                .findFirst();
    }

    private boolean estaOcupada(PosicaoPatio posicao, Set<Long> posicoesOcupadas) {
        return posicoesOcupadas.contains(posicao.getId());
    }

    private boolean ocupaPosicaoAtiva(ConteinerPatio conteiner) {
        return conteiner.getPosicao() != null
                && conteiner.getStatus() != StatusConteiner.LIBERADO
                && conteiner.getStatus() != StatusConteiner.DESPACHADO;
    }

    private boolean ehCompativel(ConteinerPatio conteiner,
                                  PosicaoPatio posicao,
                                  List<EquipamentoPatio> equipamentos) {
        String cargaDesc = conteiner.getCarga() != null && StringUtils.hasText(conteiner.getCarga().getDescricao())
                ? conteiner.getCarga().getDescricao().toUpperCase(Locale.ROOT)
                : "";

        if (cargaDesc.contains("REEFER") || cargaDesc.contains("REFRIGERADO")) {
            return equipamentos.stream()
                    .anyMatch(equipamento -> equipamento.getLinha().equals(posicao.getLinha())
                            && equipamento.getColuna().equals(posicao.getColuna()));
        }

        return true;
    }

    private int calcularRehandles(PosicaoPatio posicao, List<ConteinerPatio> conteineres) {
        return (int) conteineres.stream()
                .filter(this::ocupaPosicaoAtiva)
                .map(ConteinerPatio::getPosicao)
                .filter(posicaoOcupada -> posicaoOcupada.getColuna().equals(posicao.getColuna()))
                .filter(posicaoOcupada -> posicaoOcupada.getLinha() < posicao.getLinha())
                .count();
    }
}
