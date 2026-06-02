package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.DualCycleJobDto;
import br.com.cloudport.servicoyard.scheduler.dto.EquipmentRouteDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.servico.DualCycleOptimizationService.ContainerComPosicao;
import br.com.cloudport.servicoyard.scheduler.servico.EquipmentRouteOptimizerService.TarefaEquipamento;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

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

    public SchedulerResultDto gerarPlanoOperacional(
            VesselArrivalDto navio,
            List<String> equipamentosDisponiveis,
            List<ContainerComPosicao> containersImportacao,
            List<ContainerComPosicao> containersExportacao) {

        SchedulerResultDto resultado = new SchedulerResultDto(navio.getCodigoNavio());

        // Passo 1: Agendar janela de operação
        LocalDateTime inicioOperacao = vesselScheduler.agendar(navio);
        resultado.setObservacoes("Operação agendada para: " + inicioOperacao);

        // Passo 2: Otimizar dual-cycles (prioridade máxima)
        List<DualCycleJobDto> dualCycles = dualCycleOptimizer.otimizarDualCycles(
                equipamentosDisponiveis,
                new ArrayList<>(containersImportacao),
                new ArrayList<>(containersExportacao)
        );

        dualCycles.forEach(resultado::adicionarDualCycleJob);

        // Remover containers já usados em dual-cycle
        removerContainersUsados(containersImportacao, containersExportacao, dualCycles);

        // Passo 3: Agendar rotas para containers restantes
        List<TarefaEquipamento> tarefasRestantes = construirTarefasRestantes(
                containersImportacao,
                containersExportacao
        );

        List<EquipmentRouteDto> rotas = routeOptimizer.otimizarRotasEquipamento(
                equipamentosDisponiveis,
                tarefasRestantes
        );

        rotas.forEach(resultado::adicionarRotaEquipamento);

        // Calcular estatísticas
        resultado.calcularEstatisticas();

        return resultado;
    }

    private void removerContainersUsados(
            List<ContainerComPosicao> importacao,
            List<ContainerComPosicao> exportacao,
            List<DualCycleJobDto> dualCycles) {

        for (DualCycleJobDto dc : dualCycles) {
            importacao.removeIf(c -> c.getCodigoContainer().equals(dc.getContainerPickup()));
            exportacao.removeIf(c -> c.getCodigoContainer().equals(dc.getContainerDropoff()));
        }
    }

    private List<TarefaEquipamento> construirTarefasRestantes(
            List<ContainerComPosicao> importacao,
            List<ContainerComPosicao> exportacao) {

        List<TarefaEquipamento> tarefas = new ArrayList<>();

        for (ContainerComPosicao cont : importacao) {
            tarefas.add(new TarefaEquipamento(
                    cont.getCodigoContainer(),
                    cont.getLinha(),
                    cont.getColuna(),
                    "DESEMBARQUE"
            ));
        }

        for (ContainerComPosicao cont : exportacao) {
            tarefas.add(new TarefaEquipamento(
                    cont.getCodigoContainer(),
                    cont.getLinha(),
                    cont.getColuna(),
                    "EMBARQUE"
            ));
        }

        return tarefas;
    }

    public SchedulerResultDto gerarPlanoOperacionalDetalhado(
            VesselArrivalDto navio,
            Integer numeroEquipamentos,
            Integer containersPorEquipamento) {

        List<String> equipamentos = gerarListaEquipamentos(numeroEquipamentos);

        List<ContainerComPosicao> importacao = gerarContainersAleatorios(
                "IMP", containersPorEquipamento
        );
        List<ContainerComPosicao> exportacao = gerarContainersAleatorios(
                "EXP", containersPorEquipamento
        );

        return gerarPlanoOperacional(navio, equipamentos, importacao, exportacao);
    }

    private List<String> gerarListaEquipamentos(Integer numero) {
        List<String> equipamentos = new ArrayList<>();
        for (int i = 1; i <= numero; i++) {
            equipamentos.add("RTG_" + i);
        }
        return equipamentos;
    }

    private List<ContainerComPosicao> gerarContainersAleatorios(String prefixo, Integer numero) {
        List<ContainerComPosicao> containers = new ArrayList<>();
        for (int i = 1; i <= numero; i++) {
            int linha = (int) (Math.random() * 20);
            int coluna = (int) (Math.random() * 20);
            containers.add(new ContainerComPosicao(
                    prefixo + "_" + i,
                    linha,
                    coluna
            ));
        }
        return containers;
    }
}
