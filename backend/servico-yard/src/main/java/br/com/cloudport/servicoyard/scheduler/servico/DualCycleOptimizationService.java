package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.DualCycleConfig;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.DualCyclePair;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.YardPosition;
import br.com.cloudport.servicoyard.scheduler.dto.DualCycleJobDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Adaptador que expõe DualCycleJobDto para o PredictiveSchedulerService,
 * delegando toda a lógica ao YardDualCycleService unificado.
 */
@Service
public class DualCycleOptimizationService {

    private final YardDualCycleService dualCycleService;

    public DualCycleOptimizationService(YardDualCycleService dualCycleService) {
        this.dualCycleService = dualCycleService;
    }

    public List<DualCycleJobDto> otimizarDualCycles(
            List<String> equipamentosDisponiveis,
            List<ContainerComPosicao> containersParaPegar,
            List<ContainerComPosicao> containersParaSoltar) {

        List<YardPosition> pickups = toYardPositions(containersParaPegar, "PICKUP");
        List<YardPosition> dropoffs = toYardPositions(containersParaSoltar, "DROPOFF");
        List<DualCyclePair> pairs = dualCycleService.otimizar(pickups, dropoffs, DualCycleConfig.padrao());

        List<DualCycleJobDto> jobs = new ArrayList<>();
        int equipIndex = 0;
        for (DualCyclePair pair : pairs) {
            String equipamento = equipamentosDisponiveis.get(equipIndex % equipamentosDisponiveis.size());
            DualCycleJobDto job = new DualCycleJobDto(
                    equipamento,
                    pair.getPickup().getId(),
                    pair.getPickup().getLinha(),
                    pair.getPickup().getColuna(),
                    pair.getDropoff().getId(),
                    pair.getDropoff().getLinha(),
                    pair.getDropoff().getColuna()
            );
            job.setDistanciaTotal(job.calcularDistanciaTotal());
            job.setEconomiaDistancia(job.calcularEconomiaDistancia());
            job.setEficiencia(pair.getEconomia());
            job.setStatus("PLANEJADO");
            jobs.add(job);
            equipIndex++;
        }

        jobs.sort(Comparator.comparingDouble(DualCycleJobDto::getEficiencia).reversed());
        return jobs;
    }

    private List<YardPosition> toYardPositions(List<ContainerComPosicao> containers, String tipo) {
        List<YardPosition> positions = new ArrayList<>();
        for (ContainerComPosicao container : containers) {
            positions.add(new YardPosition(
                    container.getCodigoContainer(),
                    container.getLinha() != null ? container.getLinha() : 0,
                    container.getColuna() != null ? container.getColuna() : 0,
                    tipo
            ));
        }
        return positions;
    }

    public static class ContainerComPosicao {
        private final String codigoContainer;
        private final Integer linha;
        private final Integer coluna;
        private final String tipoOperacao;

        public ContainerComPosicao(String codigoContainer, Integer linha, Integer coluna) {
            this(codigoContainer, linha, coluna, null);
        }

        public ContainerComPosicao(String codigoContainer, Integer linha, Integer coluna, String tipoOperacao) {
            this.codigoContainer = codigoContainer;
            this.linha = linha;
            this.coluna = coluna;
            this.tipoOperacao = tipoOperacao;
        }

        public String getCodigoContainer() { return codigoContainer; }
        public Integer getLinha() { return linha; }
        public Integer getColuna() { return coluna; }
        public String getTipoOperacao() { return tipoOperacao; }
    }
}
