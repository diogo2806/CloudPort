package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.PosicaoReservaPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConsultaReservaPatioServico {

    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;

    public ConsultaReservaPatioServico(PosicaoPatioRepositorio posicaoRepositorio,
                                        ConteinerPatioRepositorio conteinerRepositorio) {
        this.posicaoRepositorio = posicaoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
    }

    @Transactional(readOnly = true)
    public List<PosicaoReservaPatioDto> listarPosicoesReservaveis() {
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        Map<Long, ConteinerPatio> conteinerPorPosicao = conteineres.stream()
                .filter(conteiner -> conteiner.getPosicao() != null && conteiner.getPosicao().getId() != null)
                .collect(Collectors.toMap(
                        conteiner -> conteiner.getPosicao().getId(),
                        Function.identity(),
                        (primeiro, ignorado) -> primeiro));
        Map<String, Long> ocupacaoPorPilha = conteineres.stream()
                .filter(conteiner -> conteiner.getPosicao() != null)
                .collect(Collectors.groupingBy(
                        conteiner -> chavePilha(conteiner.getPosicao()),
                        Collectors.counting()));

        return posicaoRepositorio.findAll(Sort.by(
                        Sort.Order.asc("linha"),
                        Sort.Order.asc("coluna"),
                        Sort.Order.asc("camadaOperacional"))).stream()
                .map(posicao -> converter(
                        posicao,
                        conteinerPorPosicao.get(posicao.getId()),
                        ocupacaoPorPilha.getOrDefault(chavePilha(posicao), 0L)))
                .toList();
    }

    private PosicaoReservaPatioDto converter(PosicaoPatio posicao,
                                              ConteinerPatio conteiner,
                                              long ocupacaoPilha) {
        return new PosicaoReservaPatioDto(
                posicao.getId(),
                posicao.getLinha(),
                posicao.getColuna(),
                posicao.getCamadaOperacional(),
                conteiner != null,
                conteiner == null ? null : conteiner.getCodigo(),
                conteiner == null ? null : conteiner.getStatus(),
                posicao.getBloco(),
                posicao.isBloqueada(),
                posicao.isInterditada(),
                posicao.isAreaPermitida(),
                posicao.getNotaOperacional(),
                tiposCargaPermitidos(posicao.getTiposCargaPermitidos()),
                posicao.getPesoMaximoToneladas(),
                posicao.getAlturaMaximaMetros(),
                posicao.getCamadaMaxima(),
                posicao.getCapacidadePilha(),
                ocupacaoPilha);
    }

    private List<String> tiposCargaPermitidos(String tiposCarga) {
        if (!StringUtils.hasText(tiposCarga)) {
            return List.of();
        }
        return Arrays.stream(tiposCarga.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String chavePilha(PosicaoPatio posicao) {
        return posicao.getLinha() + ":" + posicao.getColuna();
    }
}