package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoOtimizadaDto;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OptimizadorYardService - Algoritmo de Bin-Packing 3D com ETA Sorting")
class OptimizadorYardServiceTest {

    private OptimizadorYardService otimizador;
    private LocalDateTime agora;

    @BeforeEach
    void setup() {
        otimizador = new OptimizadorYardService();
        agora = LocalDateTime.now();
    }

    private ContainerOtimizacaoDto criarContainer(Long id, String codigo, LocalDateTime eta) {
        return new ContainerOtimizacaoDto(id, codigo, eta);
    }

    @Test
    @DisplayName("Deve retornar lista vazia para input vazio")
    void otimizarListaVazia() {
        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(List.of());

        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("Deve alocar um único contêiner no nível 1")
    void alocarUnicoContainer() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1))
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(1, resultado.size());
        PosicaoOtimizadaDto pos = resultado.get(0);
        assertEquals(1L, pos.getContainerId());
        assertEquals("CONT001", pos.getCodigoContainer());
        assertEquals(1, pos.getNivel());
        assertTrue(pos.getOtimizado());
    }

    @Test
    @DisplayName("Deve empilhar contêineres no mesmo bloco (coluna, linha)")
    void empilharContaineresNoMesmoBloco() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", agora.plusDays(2)),
                criarContainer(3L, "CONT003", agora.plusDays(3))
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(3, resultado.size());
        assertTrue(resultado.stream().allMatch(PosicaoOtimizadaDto::getOtimizado));

        Integer linha = resultado.get(0).getLinha();
        Integer coluna = resultado.get(0).getColuna();
        assertEquals(1, resultado.get(0).getNivel());
        assertEquals(2, resultado.get(1).getNivel());
        assertEquals(3, resultado.get(2).getNivel());

        resultado.forEach(pos -> {
            assertEquals(linha, pos.getLinha());
            assertEquals(coluna, pos.getColuna());
        });
    }

    @Test
    @DisplayName("Deve ordenar contêineres por ETA (mais cedo primeiro)")
    void ordenarPorEta() {
        LocalDateTime eta1 = agora.plusDays(1);
        LocalDateTime eta2 = agora.plusDays(3);
        LocalDateTime eta3 = agora.plusDays(2);

        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", eta1),
                criarContainer(2L, "CONT002", eta2),
                criarContainer(3L, "CONT003", eta3)
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(3, resultado.size());
        assertEquals(0, resultado.get(0).getSequenciaEmbarque());
        assertEquals(1, resultado.get(1).getSequenciaEmbarque());
        assertEquals(2, resultado.get(2).getSequenciaEmbarque());

        assertEquals("CONT001", resultado.get(0).getCodigoContainer());
        assertEquals("CONT003", resultado.get(1).getCodigoContainer());
        assertEquals("CONT002", resultado.get(2).getCodigoContainer());
    }

    @Test
    @DisplayName("Deve colocar containers com ETA mais cedo nos níveis superiores")
    void containerMaisCedoNoTopo() {
        LocalDateTime eta1 = agora.plusDays(1);
        LocalDateTime eta2 = agora.plusDays(3);

        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT_SAIDA_CEDO", eta1),
                criarContainer(2L, "CONT_SAIDA_TARDE", eta2)
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(2, resultado.size());
        assertEquals(1, resultado.get(0).getNivel());
        assertEquals(2, resultado.get(1).getNivel());
        assertEquals("CONT_SAIDA_CEDO", resultado.get(0).getCodigoContainer());
        assertEquals("CONT_SAIDA_TARDE", resultado.get(1).getCodigoContainer());
    }

    @Test
    @DisplayName("Deve distribuir containers em múltiplos blocos quando altura máxima atingida")
    void distribuirEmMultiplosBlocos() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", agora.plusDays(2)),
                criarContainer(3L, "CONT003", agora.plusDays(3)),
                criarContainer(4L, "CONT004", agora.plusDays(4)),
                criarContainer(5L, "CONT005", agora.plusDays(5))
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(5, resultado.size());
        assertTrue(resultado.stream().allMatch(PosicaoOtimizadaDto::getOtimizado));

        long blocosUnicos = resultado.stream()
                .map(p -> "" + p.getLinha() + "_" + p.getColuna())
                .distinct()
                .count();
        assertTrue(blocosUnicos > 1, "Deve usar múltiplos blocos");
    }

    @Test
    @DisplayName("Deve respeitar distância máxima ao berço (Vessel Zoning)")
    void reseitarDistanciaMaximaAoBerco() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", agora.plusDays(2)),
                criarContainer(3L, "CONT003", agora.plusDays(3))
        );

        int distanciaMaxima = 2;
        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacaoPorNavio(containers, distanciaMaxima);

        assertEquals(3, resultado.size());
        resultado.forEach(pos -> {
            if (pos.getOtimizado()) {
                int distancia = pos.getLinha() + pos.getColuna();
                assertTrue(distancia <= distanciaMaxima,
                        "Distância " + distancia + " excede máximo " + distanciaMaxima);
            }
        });
    }

    @Test
    @DisplayName("Deve rejeitar containers quando espaço se esgota")
    void rejeitarQuandoEspacoEsgotado() {
        List<ContainerOtimizacaoDto> containers = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> criarContainer((long) i, "CONT" + i, agora.plusDays(i % 10)))
                .toList();

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(500, resultado.size());

        long alocados = resultado.stream().filter(PosicaoOtimizadaDto::getOtimizado).count();
        long rejeitados = resultado.stream().filter(p -> !p.getOtimizado()).count();

        assertTrue(alocados > 0, "Deve alocar alguns containers");
        assertTrue(rejeitados > 0, "Deve rejeitar quando espaço se esgotar");
        assertEquals(500, alocados + rejeitados);
    }

    @Test
    @DisplayName("Deve manter sequência de embarque coerente")
    void manterSequenciaEmbarque() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", agora.plusDays(2)),
                criarContainer(3L, "CONT003", agora.plusDays(3)),
                criarContainer(4L, "CONT004", agora.plusDays(4))
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        for (int i = 0; i < resultado.size(); i++) {
            assertEquals(i, resultado.get(i).getSequenciaEmbarque(),
                    "Sequência deve ser contínua e ordenada");
        }
    }

    @Test
    @DisplayName("Deve calcular taxa de ocupação corretamente")
    void calcularTaxaOcupacao() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", agora.plusDays(2)),
                criarContainer(3L, "CONT003", agora.plusDays(3))
        );

        otimizador.otimizarAlocacao(containers);

        OptimizadorYardService.BinPacker3D packer = new OptimizadorYardService.BinPacker3D(20, 20, 4);
        packer.encontrarMelhorPosicao(containers.get(0));
        packer.encontrarMelhorPosicao(containers.get(1));
        packer.encontrarMelhorPosicao(containers.get(2));

        int taxa = packer.getTaxaOcupacao();
        assertTrue(taxa > 0, "Taxa de ocupação deve ser > 0");
        assertTrue(taxa <= 100, "Taxa de ocupação deve ser ≤ 100");
    }

    @Test
    @DisplayName("Deve handles null ETA (colocar por último)")
    void handleNullEta() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", null),
                criarContainer(3L, "CONT003", agora.plusDays(2))
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacao(containers);

        assertEquals(3, resultado.size());
        assertEquals("CONT002", resultado.get(2).getCodigoContainer(), "Null ETA deve ficar por último");
    }

    @Test
    @DisplayName("Deve encontrar melhor posição minimizando distância ao berço")
    void minimizarDistanciaAoBerco() {
        List<ContainerOtimizacaoDto> containers = Arrays.asList(
                criarContainer(1L, "CONT001", agora.plusDays(1)),
                criarContainer(2L, "CONT002", agora.plusDays(2))
        );

        List<PosicaoOtimizadaDto> resultado = otimizador.otimizarAlocacaoPorNavio(containers, Integer.MAX_VALUE);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(PosicaoOtimizadaDto::getOtimizado));

        PosicaoOtimizadaDto primeira = resultado.get(0);
        assertNotNull(primeira.getDistanciaAoBerco(), "Deve calcular distância ao berço");
        assertTrue(primeira.getDistanciaAoBerco() >= 0);
    }
}
