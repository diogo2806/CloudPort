package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerContainerDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.modelo.VesselSchedule;
import br.com.cloudport.servicoyard.scheduler.repositorio.VesselScheduleRepositorio;
import br.com.cloudport.servicoyard.scheduler.servico.DualCycleOptimizationService.ContainerComPosicao;
import br.com.cloudport.servicoyard.scheduler.servico.EquipmentRouteOptimizerService.TarefaEquipamento;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("PredictiveSchedulerService - Scheduler Preditivo de Operações")
class PredictiveSchedulerServiceTest {

    private PredictiveSchedulerService schedulerService;
    private VesselArrivalSchedulerService vesselScheduler;
    private final List<VesselSchedule> agenda = new ArrayList<>();

    private VesselArrivalDto criarNavioPadrao() {
        LocalDateTime chegada = LocalDateTime.now().plusHours(2);
        return new VesselArrivalDto(
                "MSC_GULSEUM",
                "BERCO_NORTH_1",
                chegada,
                chegada.plusHours(6),
                3,
                3
        );
    }

    @BeforeEach
    void setup() {
        VesselScheduleRepositorio repositorio = Mockito.mock(VesselScheduleRepositorio.class);
        when(repositorio.findAllByOrderByTempoPrevistoAsc()).thenAnswer(invocation -> agenda.stream()
                .sorted(Comparator.comparing(VesselSchedule::getTempoPrevisto))
                .toList());
        when(repositorio.save(any(VesselSchedule.class))).thenAnswer(invocation -> {
            VesselSchedule salvo = invocation.getArgument(0);
            agenda.add(salvo);
            return salvo;
        });
        when(repositorio.findByTempoTerminoAfterAndTempoPrevistoBeforeOrderByTempoPrevistoAsc(any(), any()))
                .thenAnswer(invocation -> {
                    LocalDateTime inicio = invocation.getArgument(0);
                    LocalDateTime fim = invocation.getArgument(1);
                    return agenda.stream()
                            .filter(item -> item.getTempoTermino().isAfter(inicio) && item.getTempoPrevisto().isBefore(fim))
                            .toList();
                });

        vesselScheduler = new VesselArrivalSchedulerService(repositorio);
        schedulerService = new PredictiveSchedulerService(
                new DualCycleOptimizationService(new YardDualCycleService()),
                new EquipmentRouteOptimizerService(),
                vesselScheduler
        );
    }

    @Test
    @DisplayName("Deve gerar plano operacional com dados reais")
    void gerarPlanoOperacionalComDadosReais() {
        VesselArrivalDto navio = criarNavioPadrao();
        List<String> equipamentos = Arrays.asList("RTG_1", "RTG_2", "RS_1");
        List<ContainerComPosicao> importacao = new ArrayList<>(Arrays.asList(
                new ContainerComPosicao("IMP001", 0, 0),
                new ContainerComPosicao("IMP002", 1, 1),
                new ContainerComPosicao("IMP003", 2, 2)
        ));
        List<ContainerComPosicao> exportacao = new ArrayList<>(Arrays.asList(
                new ContainerComPosicao("EXP001", 0, 1),
                new ContainerComPosicao("EXP002", 1, 0),
                new ContainerComPosicao("EXP003", 3, 3)
        ));

        SchedulerResultDto resultado = schedulerService.gerarPlanoOperacional(
                navio, equipamentos, importacao, exportacao
        );

        assertNotNull(resultado);
        assertEquals("MSC_GULSEUM", resultado.getCodigoNavio());
        assertTrue(resultado.getTotalOperacoes() > 0);
    }

    @Test
    @DisplayName("Deve aceitar contrato operacional sem gerar posições aleatórias")
    void gerarPlanoPeloContratoOperacional() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = new SchedulerPlanoOperacionalRequisicaoDto();
        requisicao.setNavio(criarNavioPadrao());
        requisicao.setEquipamentosDisponiveis(List.of("RTG_REAL_01"));
        requisicao.setContainersImportacao(List.of(
                new SchedulerContainerDto("IMP001", 1, 1),
                new SchedulerContainerDto("IMP002", 2, 2),
                new SchedulerContainerDto("IMP003", 3, 3)
        ));
        requisicao.setContainersExportacao(List.of(
                new SchedulerContainerDto("EXP001", 1, 2),
                new SchedulerContainerDto("EXP002", 2, 1),
                new SchedulerContainerDto("EXP003", 4, 4)
        ));

        SchedulerResultDto resultado = schedulerService.gerarPlanoOperacional(requisicao);

        assertNotNull(resultado);
        assertEquals("MSC_GULSEUM", resultado.getCodigoNavio());
        assertTrue(resultado.getTotalOperacoes() > 0);
    }

    @Test
    @DisplayName("Deve rejeitar quantidade manifestada diferente da lista")
    void rejeitarQuantidadeInconsistente() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = new SchedulerPlanoOperacionalRequisicaoDto();
        requisicao.setNavio(criarNavioPadrao());
        requisicao.setEquipamentosDisponiveis(List.of("RTG_REAL_01"));
        requisicao.setContainersImportacao(List.of(new SchedulerContainerDto("IMP001", 1, 1)));
        requisicao.setContainersExportacao(List.of(new SchedulerContainerDto("EXP001", 1, 2)));

        assertThrows(IllegalArgumentException.class, () -> schedulerService.gerarPlanoOperacional(requisicao));
    }

    @Test
    @DisplayName("Deve permitir horários simultâneos em berços diferentes")
    void permitirHorariosSimultaneosEmBercosDiferentes() {
        LocalDateTime inicio = LocalDateTime.now().plusHours(1);
        VesselArrivalDto primeiro = new VesselArrivalDto("NAVIO_A", "BERCO_1", inicio, inicio.plusHours(4), 10, 10);
        VesselArrivalDto segundo = new VesselArrivalDto("NAVIO_B", "BERCO_2", inicio, inicio.plusHours(4), 10, 10);

        assertEquals(inicio, vesselScheduler.agendar(primeiro));
        assertEquals(inicio, vesselScheduler.agendar(segundo));
    }

    @Test
    @DisplayName("Deve deslocar conflito no mesmo berço")
    void deslocarConflitoNoMesmoBerco() {
        LocalDateTime inicio = LocalDateTime.now().plusHours(1);
        VesselArrivalDto primeiro = new VesselArrivalDto("NAVIO_A", "BERCO_1", inicio, inicio.plusHours(4), 10, 10);
        VesselArrivalDto segundo = new VesselArrivalDto("NAVIO_B", "BERCO_1", inicio.plusHours(1), inicio.plusHours(5), 10, 10);

        LocalDateTime slotPrimeiro = vesselScheduler.agendar(primeiro);
        LocalDateTime slotSegundo = vesselScheduler.agendar(segundo);

        assertEquals(inicio, slotPrimeiro);
        assertTrue(!slotSegundo.isBefore(inicio.plusHours(4)));
    }

    @Test
    @DisplayName("Deve otimizar rotas com sequência minimizada")
    void otimizarRotasComSequenciaMinimizada() {
        List<TarefaEquipamento> tarefas = Arrays.asList(
                new TarefaEquipamento("CONT001", 5, 5, "EMBARQUE"),
                new TarefaEquipamento("CONT002", 1, 1, "EMBARQUE"),
                new TarefaEquipamento("CONT003", 3, 3, "EMBARQUE")
        );

        var rotas = new EquipmentRouteOptimizerService()
                .otimizarRotasEquipamento(List.of("RTG_1"), tarefas);

        assertTrue(!rotas.isEmpty());
        assertEquals(3, rotas.get(0).getParadas().size());
        assertEquals("RTG_1", rotas.get(0).getEquipamentoId());
    }

    @Test
    @DisplayName("Deve calcular capacidade requerida corretamente")
    void calcularCapacidadeRequerida() {
        LocalDateTime inicio = LocalDateTime.now();
        VesselArrivalDto navio = new VesselArrivalDto("TEST", "BERCO_X", inicio, inicio.plusHours(5), 20, 30);
        assertEquals(50, vesselScheduler.calcularCapacidadeRequerida(navio));
    }
}
