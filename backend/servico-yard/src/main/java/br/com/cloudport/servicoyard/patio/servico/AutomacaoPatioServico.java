package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.RespostaAutoplanejamentoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        List<String> containersPlanificados = new ArrayList<>();
        List<String> containersException = new ArrayList<>();

        for (ConteinerPatio conteiner : conteineres) {
            if (ehCandidatoAAutoplanejamento(conteiner)) {
                try {
                    Optional<PosicaoPatio> melhorPosicao = encontrarMelhorPosicao(conteiner, posicoes, equipamentos);

                    if (melhorPosicao.isPresent()) {
                        conteiner.setPosicao(melhorPosicao.get());
                        conteinerPatioRepositorio.save(conteiner);
                        containersPlanificados.add(conteiner.getCodigo());
                    } else {
                        containersException.add(conteiner.getCodigo() + " (sem posição disponível)");
                    }
                } catch (Exception e) {
                    containersException.add(conteiner.getCodigo() + " (erro: " + e.getMessage() + ")");
                }
            }
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
        if (conteiner.getCarga() == null) {
            return false;
        }

        String descricao = conteiner.getCarga().getDescricao().toUpperCase();
        return !descricao.contains("PERIGOSA") && !descricao.contains("IMO");
    }

    private Optional<PosicaoPatio> encontrarMelhorPosicao(ConteinerPatio conteiner,
                                                           List<PosicaoPatio> posicoes,
                                                           List<EquipamentoPatio> equipamentos) {
        List<ConteinerPatio> todosConteineres = conteinerPatioRepositorio.findAll();

        return posicoes.stream()
            .filter(p -> !estaOcupada(p, todosConteineres))
            .filter(p -> ehCompativel(conteiner, p, equipamentos))
            .sorted((p1, p2) -> {
                int rehp1 = calcularRehandles(p1, todosConteineres);
                int rehp2 = calcularRehandles(p2, todosConteineres);
                return Integer.compare(rehp1, rehp2);
            })
            .findFirst();
    }

    private boolean estaOcupada(PosicaoPatio posicao, List<ConteinerPatio> conteineres) {
        return conteineres.stream()
            .anyMatch(c -> c.getPosicao().getId().equals(posicao.getId()));
    }

    private boolean ehCompativel(ConteinerPatio conteiner, PosicaoPatio posicao, List<EquipamentoPatio> equipamentos) {
        String cargaDesc = conteiner.getCarga() != null
            ? conteiner.getCarga().getDescricao().toUpperCase()
            : "";

        if (cargaDesc.contains("REEFER") || cargaDesc.contains("REFRIGERADO")) {
            return equipamentos.stream()
                .anyMatch(e -> e.getLinha().equals(posicao.getLinha())
                    && e.getColuna().equals(posicao.getColuna()));
        }

        return true;
    }

    private int calcularRehandles(PosicaoPatio posicao, List<ConteinerPatio> conteineres) {
        return (int) conteineres.stream()
            .filter(c -> c.getPosicao().getColuna().equals(posicao.getColuna()))
            .filter(c -> c.getPosicao().getLinha() < posicao.getLinha())
            .count();
    }
}
