package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.DualCycleJobDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import br.com.cloudport.servicoyard.patio.util.YardDistanceCalculator;
import org.springframework.stereotype.Service;

@Service
public class DualCycleOptimizationService {

    private static final Integer DISTANCIA_MAXIMA_PARA_PAREAMENTO = 10;
    private static final Double ECONOMIA_MINIMA_PERCENTUAL = 15.0;

    public List<DualCycleJobDto> otimizarDualCycles(
            List<String> equipamentosDisponiveis,
            List<ContainerComPosicao> containersParaPegar,
            List<ContainerComPosicao> containersParaSoltar) {

        List<DualCycleJobDto> dualCycles = new ArrayList<>();

        for (String equipamento : equipamentosDisponiveis) {
            List<DualCycleJobDto> cyclesEquipamento = encontrarMelhoresCombinacoes(
                    equipamento,
                    containersParaPegar,
                    containersParaSoltar
            );
            dualCycles.addAll(cyclesEquipamento);

            removerContainersUsados(containersParaPegar, containersParaSoltar, cyclesEquipamento);
        }

        return dualCycles.stream()
                .sorted(Comparator.comparingDouble(
                        (DualCycleJobDto j) -> j.calcularEficiencia()).reversed())
                .toList();
    }

    private List<DualCycleJobDto> encontrarMelhoresCombinacoes(
            String equipamento,
            List<ContainerComPosicao> pickups,
            List<ContainerComPosicao> dropoffs) {

        List<DualCycleJobDto> resultado = new ArrayList<>();
        List<ContainerComPosicao> pickupsDisponiveis = new ArrayList<>(pickups);

        for (ContainerComPosicao pickup : pickupsDisponiveis) {
            DualCycleJobDto melhorCombo = null;
            Integer menorDistancia = Integer.MAX_VALUE;

            for (ContainerComPosicao dropoff : dropoffs) {
                Integer distancia = calcularDistancia(
                        pickup.getLinha(), pickup.getColuna(),
                        dropoff.getLinha(), dropoff.getColuna()
                );

                if (distancia <= DISTANCIA_MAXIMA_PARA_PAREAMENTO &&
                    distancia < menorDistancia) {

                    DualCycleJobDto combo = criarDualCycleJob(
                            equipamento, pickup, dropoff, distancia
                    );

                    if (combo.calcularEficiencia() >= ECONOMIA_MINIMA_PERCENTUAL) {
                        melhorCombo = combo;
                        menorDistancia = distancia;
                    }
                }
            }

            if (melhorCombo != null) {
                resultado.add(melhorCombo);
                dropoffs.remove(melhorCombo.getLinhaDropoff());
            }
        }

        return resultado;
    }

    private DualCycleJobDto criarDualCycleJob(
            String equipamento,
            ContainerComPosicao pickup,
            ContainerComPosicao dropoff,
            Integer distancia) {

        DualCycleJobDto job = new DualCycleJobDto(
                equipamento,
                pickup.getCodigoContainer(),
                pickup.getLinha(),
                pickup.getColuna(),
                dropoff.getCodigoContainer(),
                dropoff.getLinha(),
                dropoff.getColuna()
        );

        job.setDistanciaTotal(job.calcularDistanciaTotal());
        job.setEconomiaDistancia(job.calcularEconomiaDistancia());
        job.setEficiencia(job.calcularEficiencia());
        job.setStatus("PLANEJADO");

        return job;
    }

    private Integer calcularDistancia(Integer l1, Integer c1, Integer l2, Integer c2) {
        return YardDistanceCalculator.manhattan(l1, c1, l2, c2);
    }

    private void removerContainersUsados(
            List<ContainerComPosicao> pickups,
            List<ContainerComPosicao> dropoffs,
            List<DualCycleJobDto> usados) {

        for (DualCycleJobDto job : usados) {
            pickups.removeIf(p -> p.getCodigoContainer().equals(job.getContainerPickup()));
            dropoffs.removeIf(d -> d.getCodigoContainer().equals(job.getContainerDropoff()));
        }
    }

    public static class ContainerComPosicao {
        private String codigoContainer;
        private Integer linha;
        private Integer coluna;
        private String tipoOperacao;

        public ContainerComPosicao(String codigoContainer, Integer linha, Integer coluna) {
            this.codigoContainer = codigoContainer;
            this.linha = linha;
            this.coluna = coluna;
        }

        public ContainerComPosicao(String codigoContainer, Integer linha, Integer coluna,
                                   String tipoOperacao) {
            this.codigoContainer = codigoContainer;
            this.linha = linha;
            this.coluna = coluna;
            this.tipoOperacao = tipoOperacao;
        }

        public String getCodigoContainer() {
            return codigoContainer;
        }

        public Integer getLinha() {
            return linha;
        }

        public Integer getColuna() {
            return coluna;
        }

        public String getTipoOperacao() {
            return tipoOperacao;
        }
    }
}
