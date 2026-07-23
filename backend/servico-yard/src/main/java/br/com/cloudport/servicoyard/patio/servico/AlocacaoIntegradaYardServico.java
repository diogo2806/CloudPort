package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoOtimizadaDto;
import br.com.cloudport.servicoyard.patio.dto.RespostaAlocacaoIntegradaYardDto;
import br.com.cloudport.servicoyard.patio.dto.ResultadoAlocacaoYardDto;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlocacaoIntegradaYardServico {

    private final OptimizadorYardService otimizador;
    private final ValidadorYardPlacementService validador;
    private final MapaPatioServico mapaPatio;

    public AlocacaoIntegradaYardServico(OptimizadorYardService otimizador,
                                        ValidadorYardPlacementService validador,
                                        MapaPatioServico mapaPatio) {
        this.otimizador = otimizador;
        this.validador = validador;
        this.mapaPatio = mapaPatio;
    }

    @Transactional
    public RespostaAlocacaoIntegradaYardDto alocar(List<ContainerOtimizacaoDto> conteineres) {
        List<ContainerOtimizacaoDto> entrada = conteineres == null ? List.of() : conteineres;
        Map<Long, ContainerOtimizacaoDto> porId = entrada.stream()
                .collect(Collectors.toMap(ContainerOtimizacaoDto::getId, Function.identity(), (primeiro, ignorado) -> primeiro));
        List<ResultadoAlocacaoYardDto> resultados = new ArrayList<>();

        for (PosicaoOtimizadaDto posicao : otimizador.otimizarAlocacao(entrada)) {
            if (!Boolean.TRUE.equals(posicao.getOtimizado())) {
                resultados.add(ResultadoAlocacaoYardDto.rejeitado(posicao, posicao.getMotivo()));
                continue;
            }

            ContainerOtimizacaoDto origem = porId.get(posicao.getContainerId());
            ConteinerPatioRequisicaoDto requisicao = mapear(origem, posicao);
            try {
                validador.validarAlocacao(requisicao);
                mapaPatio.registrarOuAtualizarConteiner(requisicao);
                resultados.add(ResultadoAlocacaoYardDto.alocado(posicao));
            } catch (IllegalArgumentException validacaoOperacional) {
                resultados.add(ResultadoAlocacaoYardDto.rejeitado(posicao, validacaoOperacional.getMessage()));
            }
        }

        int alocados = (int) resultados.stream().filter(item -> "ALOCADO".equals(item.status())).count();
        return new RespostaAlocacaoIntegradaYardDto(entrada.size(), alocados, resultados.size() - alocados, List.copyOf(resultados));
    }

    private ConteinerPatioRequisicaoDto mapear(ContainerOtimizacaoDto origem, PosicaoOtimizadaDto posicao) {
        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setId(origem.getId());
        requisicao.setCodigo(origem.getCodigo());
        requisicao.setLinha(posicao.getLinha());
        requisicao.setColuna(posicao.getColuna());
        requisicao.setCamadaOperacional(String.valueOf(posicao.getNivel()));
        requisicao.setStatus(StatusConteiner.ALOCADO);
        requisicao.setTipoCarga(origem.getTipoCarga());
        requisicao.setDestino(origem.getDestino());
        return requisicao;
    }
}
