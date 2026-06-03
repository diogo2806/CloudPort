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
            String equipamento = equipamentosDisponiveis.isEmpty()
                    ? "EQUIP_" + equipIndex
                    : equipamentosDisponiveis.get(equipIndex % equipamentosDisponiveis.size());

            DualCycleJobDto job = new DualCycleJobDto(
                    equipamento,
                    pair.getPickup().getId(),
                    Integer.parseInt(pair.getPickup().getId().contains("@")
                            ? pair.getPickup().getId().split("@")[1].split(",")[0] : "0"),
                    Integer.parseInt(pair.getPickup().getId().contains("@")
                            ? pair.getPickup().getId().split("@")[1].split(",")[1] : "0"),
                    pair.getDropoff().getId(),
                    Integer.parseInt(pair.getDropoff().getId().contains("@")
                            ? pair.getDropoff().getId().split("@")[1].split(",")[0] : "0"),
                    Integer.parseInt(pair.getDropoff().getId().contains("@")
                            ? pair.getDropoff().getId().split("@")[1].split(",")[1] : "0")
            );

            // Reconstruir com posições corretas via pickup/dropoff diretos
            DualCycleJobDto jobCorreto = new DualCycleJobDto(
                    equipamento,
                    pair.getPickup().getId(),
                    pair.getPickup().getLinha(),
                    pair.getPickup().getColuna(),
                    pair.getDropoff().getId(),
                    pair.getDropoff().getLinha(),
                    pair.getDropoff().getColuna()
            );
            jobCorreto.setDistanciaTotal(jobCorreto.calcularDistanciaTotal());
            jobCorreto.setEconomiaDistancia(jobCorreto.calcularEconomiaDistancia());
            jobCorreto.setEficiencia(pair.getEconomia());
            jobCorreto.setStatus("PLANEJADO");
            jobs.add(jobCorreto);
            equipIndex++;
        }

        jobs.sort(Comparator.comparingDouble(DualCycleJobDto::getEficiencia).reversed());
        return jobs;
    }

    private List<YardPosition> toYardPositions(List<ContainerComPosicao> containers, String tipo) {
        List<YardPosition> positions = new ArrayList<>();
        for (ContainerComPosicao c : containers) {
            positions.add(new YardPosition(c.getCodigoContainer(),
                    c.getLinha() != null ? c.getLinha() : 0,
                    c.getColuna() != null ? c.getColuna() : 0,
                    tipo));
        }
        return positions;
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
