package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.DualCycleJobDto;
import br.com.cloudport.servicoyard.scheduler.dto.EquipmentRouteDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerContainerDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.servico.DualCycleOptimizationService.ContainerComPosicao;
import br.com.cloudport.servicoyard.scheduler.servico.EquipmentRouteOptimizerService.TarefaEquipamento;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PredictiveSchedulerService {

    private final DualCycleOptimizationService dualCycleOptimizer;
    private final EquipmentRouteOptimizerService routeOptimizer;
    private final VesselArrivalSchedulerService vesselScheduler;

    public PredictiveSchedulerService(
            DualCycleOptimizationService dualCycleOptimizer,
            EquipmentRouteOptimizerService routeOptimizer,
            VesselArrivalSchedulerService vesselScheduler) {
        this.dualCycleOptimizer = dualCycleOptimizer;
        this.routeOptimizer = routeOptimizer;
        this.vesselScheduler = vesselScheduler;
    }

    public SchedulerResultDto gerarPlanoOperacional(SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        if (requisicao == null || requisicao.getNavio() == null) {
            throw new IllegalArgumentException("Os dados reais do navio devem ser informados.");
        }
        List<String> equipamentos = normalizarEquipamentos(requisicao.getEquipamentosDisponiveis());
        List<ContainerComPosicao> importacao = converterContainers(requisicao.getContainersImportacao());
        List<ContainerComPosicao> exportacao = converterContainers(requisicao.getContainersExportacao());
        validarTotaisManifestados(requisicao.getNavio(), importacao, exportacao);
        return gerarPlanoOperacional(requisicao.getNavio(), equipamentos, importacao, exportacao);
    }

    public SchedulerResultDto gerarPlanoOperacional(
            VesselArrivalDto navio,
            List<String> equipamentosDisponiveis,
            List<ContainerComPosicao> containersImportacao,
            List<ContainerComPosicao> containersExportacao) {

        validarNavio(navio);
        List<String> equipamentos = normalizarEquipamentos(equipamentosDisponiveis);
        List<ContainerComPosicao> importacao = new ArrayList<>(containersImportacao == null ? List.of() : containersImportacao);
        List<ContainerComPosicao> exportacao = new ArrayList<>(containersExportacao == null ? List.of() : containersExportacao);

        SchedulerResultDto resultado = new SchedulerResultDto(navio.getCodigoNavio());

        LocalDateTime inicioOperacao = vesselScheduler.agendar(navio);
        resultado.setObservacoes("Operação agendada para: " + inicioOperacao);

        List<DualCycleJobDto> dualCycles = dualCycleOptimizer.otimizarDualCycles(
                equipamentos,
                new ArrayList<>(importacao),
                new ArrayList<>(exportacao)
        );
        dualCycles.forEach(resultado::adicionarDualCycleJob);

        removerContainersUsados(importacao, exportacao, dualCycles);

        List<TarefaEquipamento> tarefasRestantes = construirTarefasRestantes(importacao, exportacao);
        List<EquipmentRouteDto> rotas = routeOptimizer.otimizarRotasEquipamento(equipamentos, tarefasRestantes);
        rotas.forEach(resultado::adicionarRotaEquipamento);

        resultado.calcularEstatisticas();
        return resultado;
    }

    private void validarNavio(VesselArrivalDto navio) {
        if (navio == null) {
            throw new IllegalArgumentException("O navio deve ser informado.");
        }
        if (navio.getEtaChegada() == null || navio.getEtaPartida() == null
                || !navio.getEtaPartida().isAfter(navio.getEtaChegada())) {
            throw new IllegalArgumentException("A ETA de partida deve ser posterior à ETA de chegada.");
        }
    }

    private void validarTotaisManifestados(VesselArrivalDto navio,
                                             List<ContainerComPosicao> importacao,
                                             List<ContainerComPosicao> exportacao) {
        if (navio.getQuantidadeContainersImportacao() != null
                && navio.getQuantidadeContainersImportacao() != importacao.size()) {
            throw new IllegalArgumentException("A quantidade de contêineres de importação difere da lista informada.");
        }
        if (navio.getQuantidadeContainersExportacao() != null
                && navio.getQuantidadeContainersExportacao() != exportacao.size()) {
            throw new IllegalArgumentException("A quantidade de contêineres de exportação difere da lista informada.");
        }
    }

    private List<String> normalizarEquipamentos(List<String> equipamentos) {
        List<String> normalizados = (equipamentos == null ? List.<String>of() : equipamentos).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizados.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um equipamento real disponível.");
        }
        return normalizados;
    }

    private List<ContainerComPosicao> converterContainers(List<SchedulerContainerDto> containers) {
        return (containers == null ? List.<SchedulerContainerDto>of() : containers).stream()
                .map(container -> new ContainerComPosicao(
                        container.getCodigoContainer(),
                        container.getLinha(),
                        container.getColuna()
                ))
                .toList();
    }

    private void removerContainersUsados(
            List<ContainerComPosicao> importacao,
            List<ContainerComPosicao> exportacao,
            List<DualCycleJobDto> dualCycles) {

        for (DualCycleJobDto dualCycle : dualCycles) {
            importacao.removeIf(container -> container.getCodigoContainer().equals(dualCycle.getContainerPickup()));
            exportacao.removeIf(container -> container.getCodigoContainer().equals(dualCycle.getContainerDropoff()));
        }
    }

    private List<TarefaEquipamento> construirTarefasRestantes(
            List<ContainerComPosicao> importacao,
            List<ContainerComPosicao> exportacao) {

        List<TarefaEquipamento> tarefas = new ArrayList<>();

        for (ContainerComPosicao container : importacao) {
            tarefas.add(new TarefaEquipamento(
                    container.getCodigoContainer(),
                    container.getLinha(),
                    container.getColuna(),
                    "DESEMBARQUE"
            ));
        }

        for (ContainerComPosicao container : exportacao) {
            tarefas.add(new TarefaEquipamento(
                    container.getCodigoContainer(),
                    container.getLinha(),
                    container.getColuna(),
                    "EMBARQUE"
            ));
        }

        return tarefas;
    }
}
