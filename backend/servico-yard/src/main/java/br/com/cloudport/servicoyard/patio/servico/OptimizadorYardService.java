package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoOtimizadaDto;
import br.com.cloudport.servicoyard.patio.util.YardConstants;
import br.com.cloudport.servicoyard.patio.util.YardDistanceCalculator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OptimizadorYardService {

    private static final int LARGURA_GRID = 20;
    private static final int COMPRIMENTO_GRID = 20;

    public List<PosicaoOtimizadaDto> otimizarAlocacao(List<ContainerOtimizacaoDto> conteineres) {
        if (conteineres == null || conteineres.isEmpty()) {
            return new ArrayList<>();
        }

        List<ContainerOtimizacaoDto> containeresOrdenados = ordenarPorEta(conteineres);
        BinPacker3D binPacker = new BinPacker3D(
                LARGURA_GRID, COMPRIMENTO_GRID, YardConstants.EMPILHAMENTO_MAXIMO);

        List<PosicaoOtimizadaDto> resultado = new ArrayList<>();
        int containerIndex = 0;

        for (ContainerOtimizacaoDto container : containeresOrdenados) {
            Optional<PosicaoOtimizadaDto> posicao = binPacker.encontrarMelhorPosicao(container);

            if (posicao.isPresent()) {
                PosicaoOtimizadaDto pos = posicao.get();
                pos.setSequenciaEmbarque(containerIndex++);
                pos.setOtimizado(true);
                resultado.add(pos);
            } else {
                resultado.add(criarPosicaoRejeitada(container, containerIndex++, "Espaço indisponível no pátio"));
            }
        }

        return resultado;
    }

    public List<PosicaoOtimizadaDto> otimizarAlocacaoPorNavio(List<ContainerOtimizacaoDto> conteineres,
                                                                Integer distanciaMaximaAoBerco) {
        if (conteineres == null || conteineres.isEmpty()) {
            return new ArrayList<>();
        }

        List<ContainerOtimizacaoDto> containeresOrdenados = ordenarPorEta(conteineres);
        BinPacker3D binPacker = new BinPacker3D(
                LARGURA_GRID, COMPRIMENTO_GRID, YardConstants.EMPILHAMENTO_MAXIMO);

        List<PosicaoOtimizadaDto> resultado = new ArrayList<>();
        int containerIndex = 0;

        for (ContainerOtimizacaoDto container : containeresOrdenados) {
            Optional<PosicaoOtimizadaDto> posicao = binPacker.encontrarMelhorPosicao(
                    container,
                    distanciaMaximaAoBerco
            );

            if (posicao.isPresent()) {
                PosicaoOtimizadaDto pos = posicao.get();
                pos.setSequenciaEmbarque(containerIndex++);
                pos.setOtimizado(true);
                pos.setDistanciaAoBerco(calcularDistancia(pos.getLinha(), pos.getColuna()));
                resultado.add(pos);
            } else {
                resultado.add(criarPosicaoRejeitada(container, containerIndex++, "Sem espaço na zona de embarque"));
            }
        }

        return resultado;
    }

    private List<ContainerOtimizacaoDto> ordenarPorEta(List<ContainerOtimizacaoDto> conteineres) {
        return conteineres.stream()
                .sorted(Comparator
                        .nullsLast(Comparator.comparing(ContainerOtimizacaoDto::getEtaPartida))
                        .thenComparing(ContainerOtimizacaoDto::getId))
                .toList();
    }

    private PosicaoOtimizadaDto criarPosicaoRejeitada(ContainerOtimizacaoDto container,
                                                        int sequencia,
                                                        String motivo) {
        return new PosicaoOtimizadaDto(
                container.getId(),
                container.getCodigo(),
                null,
                null,
                null,
                sequencia,
                false,
                motivo
        );
    }

    private Integer calcularDistancia(Integer linha, Integer coluna) {
        if (linha == null || coluna == null) {
            return Integer.MAX_VALUE;
        }
        return YardDistanceCalculator.fromOrigin(linha, coluna);
    }

    public static class BinPacker3D {

        private final int largura;
        private final int comprimento;
        private final int altura;
        private final boolean[][][] grid;
        private final List<PosicaoOcupada> posicoes;

        public BinPacker3D(int largura, int comprimento, int altura) {
            this.largura = largura;
            this.comprimento = comprimento;
            this.altura = altura;
            this.grid = new boolean[largura][comprimento][altura];
            this.posicoes = new ArrayList<>();
        }

        public Optional<PosicaoOtimizadaDto> encontrarMelhorPosicao(ContainerOtimizacaoDto container) {
            return encontrarMelhorPosicao(container, Integer.MAX_VALUE);
        }

        public Optional<PosicaoOtimizadaDto> encontrarMelhorPosicao(ContainerOtimizacaoDto container,
                                                                     Integer distanciaMaxima) {
            Integer melhorLinha = null;
            Integer melhorColuna = null;
            Integer melhorNivel = null;
            Integer melhorDistancia = Integer.MAX_VALUE;

            for (int col = 0; col < comprimento; col++) {
                for (int lin = 0; lin < largura; lin++) {
                    int distancia = YardDistanceCalculator.fromOrigin(lin, col);

                    if (distancia > distanciaMaxima) {
                        continue;
                    }

                    for (int niv = 0; niv < altura; niv++) {
                        if (!grid[lin][col][niv]) {
                            if (niv == 0 || grid[lin][col][niv - 1]) {
                                if (distancia < melhorDistancia) {
                                    melhorLinha = lin;
                                    melhorColuna = col;
                                    melhorNivel = niv;
                                    melhorDistancia = distancia;
                                }
                                break;
                            }
                        }
                    }
                }
            }

            if (melhorLinha != null && melhorColuna != null && melhorNivel != null) {
                grid[melhorLinha][melhorColuna][melhorNivel] = true;
                posicoes.add(new PosicaoOcupada(melhorLinha, melhorColuna, melhorNivel, container.getCodigo()));

                return Optional.of(new PosicaoOtimizadaDto(
                        container.getId(),
                        container.getCodigo(),
                        melhorLinha,
                        melhorColuna,
                        melhorNivel + 1,
                        0,
                        true,
                        null
                ));
            }

            return Optional.empty();
        }

        public void liberarPosicao(Integer linha, Integer coluna, Integer nivel) {
            if (linha != null && coluna != null && nivel != null && nivel > 0 && nivel <= altura) {
                grid[linha][coluna][nivel - 1] = false;
                posicoes.removeIf(p -> p.linha == linha && p.coluna == coluna && p.nivel == (nivel - 1));
            }
        }

        public int getTaxaOcupacao() {
            long ocupadas = posicoes.size();
            long total = (long) largura * comprimento * altura;
            return (int) ((ocupadas * 100) / total);
        }

        public List<PosicaoOcupada> getPosicoes() {
            return new ArrayList<>(posicoes);
        }

        public static class PosicaoOcupada {
            public final int linha;
            public final int coluna;
            public final int nivel;
            public final String codigoContainer;

            public PosicaoOcupada(int linha, int coluna, int nivel, String codigoContainer) {
                this.linha = linha;
                this.coluna = coluna;
                this.nivel = nivel;
                this.codigoContainer = codigoContainer;
            }
        }
    }
}
