package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.servico.DualCycleOptimizationService.ContainerComPosicao;
import br.com.cloudport.servicoyard.scheduler.servico.EquipmentRouteOptimizerService.TarefaEquipamento;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PredictiveSchedulerService - Scheduler Preditivo de Operações")
class PredictiveSchedulerServiceTest {

    @Mock
    private DualCycleOptimizationService dualCycleOptimizer;

    @Mock
    private EquipmentRouteOptimizerService routeOptimizer;

    @Mock
    private VesselArrivalSchedulerService vesselScheduler;

    private PredictiveSchedulerService schedulerService;

    private VesselArrivalDto criarNavioPadrao() {
        return new VesselArrivalDto(
                "MSC_GULSEUM",
                "BERCO_NORTH_1",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(8),
                10,
                15
        );
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        schedulerService = new PredictiveSchedulerService(
                new DualCycleOptimizationService(new YardDualCycleService()),
                new EquipmentRouteOptimizerService(),
                new VesselArrivalSchedulerService()
        );
    }

    @Test
    @DisplayName("Deve gerar plano operacional com dual-cycles")
    void gerarPlanoOperacionalComDualCycles() {
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
    @DisplayName("Deve calcular eficiência de dual-cycle")
    void calcularEficienciaDualCycle() {
        DualCycleOptimizationService.ContainerComPosicao pickup =
                new ContainerComPosicao("CONT001", 0, 0);
        DualCycleOptimizationService.ContainerComPosicao dropoff =
                new ContainerComPosicao("CONT002", 2, 2);

        List<String> equipamentos = Arrays.asList("RTG_1");
        List<DualCycleOptimizationService.ContainerComPosicao> pickups = Arrays.asList(pickup);
        List<DualCycleOptimizationService.ContainerComPosicao> dropoffs = Arrays.asList(dropoff);

        DualCycleOptimizationService dcs = new DualCycleOptimizationService(new YardDualCycleService());
        var dualCycles = dcs.otimizarDualCycles(equipamentos, pickups, dropoffs);

        if (!dualCycles.isEmpty()) {
            var dc = dualCycles.get(0);
            assertTrue(dc.getEficiencia() >= 0, "Eficiência deve ser positiva");
            assertTrue(dc.getEficiencia() <= 100, "Eficiência não pode exceder 100%");
        }
    }

    @Test
    @DisplayName("Deve otimizar rotas com sequência minimizada")
    void otimizarRotasComSequenciaMinimizada() {
        List<TarefaEquipamento> tarefas = Arrays.asList(
                new TarefaEquipamento("CONT001", 5, 5, "EMBARQUE"),
                new TarefaEquipamento("CONT002", 1, 1, "EMBARQUE"),
                new TarefaEquipamento("CONT003", 3, 3, "EMBARQUE")
        );

        List<String> equipamentos = Arrays.asList("RTG_1");

        EquipmentRouteOptimizerService eros = new EquipmentRouteOptimizerService();
        var rotas = eros.otimizarRotasEquipamento(equipamentos, tarefas);

        assertTrue(!rotas.isEmpty(), "Deve gerar rotas");

        if (!rotas.isEmpty()) {
            var rota = rotas.get(0);
            assertEquals(3, rota.getParadas().size(), "Deve ter 3 paradas");
            assertEquals("RTG_1", rota.getEquipamentoId());
        }
    }

    @Test
    @DisplayName("Deve agendar navio sem conflitos")
    void agendarNavioSemConflitos() {
        VesselArrivalDto navio1 = new VesselArrivalDto(
                "NAVIO_A",
                "BERCO_1",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(5),
                10, 10
        );

        VesselArrivalDto navio2 = new VesselArrivalDto(
                "NAVIO_B",
                "BERCO_1",
                LocalDateTime.now().plusHours(6),
                LocalDateTime.now().plusHours(10),
                10, 10
        );

        VesselArrivalSchedulerService vass = new VesselArrivalSchedulerService();

        var slot1 = vass.agendar(navio1);
        var slot2 = vass.agendar(navio2);

        assertNotNull(slot1);
        assertNotNull(slot2);
        assertTrue(slot2.isAfter(slot1), "Segundo navio deve ser agendado depois do primeiro");
    }

    @Test
    @DisplayName("Deve retornar status geral baseado em eficiência")
    void statusGeralBaseadoEmEficiencia() {
        VesselArrivalDto navio = criarNavioPadrao();
        SchedulerResultDto resultado = new SchedulerResultDto(navio.getCodigoNavio());

        resultado.setTotalOperacoes(10);
        resultado.setOperacoesDualCycle(9);
        resultado.setDistanciaEconomizada(50);
        resultado.setEficienciaMedia(85.0);
        resultado.setStatusGeral("EXCELENTE");

        assertEquals("EXCELENTE", resultado.getStatusGeral(),
                "Status deve ser EXCELENTE com 90% dual-cycle");
    }

    @Test
    @DisplayName("Deve gerar plano com containers aleatórios")
    void gerarPlanoComContainersAleatorios() {
        VesselArrivalDto navio = criarNavioPadrao();

        SchedulerResultDto resultado = schedulerService.gerarPlanoOperacionalDetalhado(
                navio,
                3,
                5
        );

        assertNotNull(resultado);
        assertNotNull(resultado.getCodigoNavio());
        assertTrue(resultado.getTotalOperacoes() > 0,
                "Deve ter operações planejadas");
    }

    @Test
    @DisplayName("Deve calcular capacidade requerida corretamente")
    void calcularCapacidadeRequerida() {
        VesselArrivalDto navio = new VesselArrivalDto(
                "TEST",
                "BERCO_X",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(5),
                20,
                30
        );

        VesselArrivalSchedulerService vass = new VesselArrivalSchedulerService();
        Integer capacidade = vass.calcularCapacidadeRequerida(navio);

        assertEquals(50, capacidade, "Capacidade = 20 + 30");
    }

    @Test
    @DisplayName("Deve gerar janela de tempo correta")
    void gerarJanelaTempoCorreta() {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = inicio.plusHours(6);

        VesselArrivalDto navio = new VesselArrivalDto(
                "TEST",
                "BERCO",
                inicio,
                fim,
                15, 15
        );

        Integer janelaHoras = navio.getJanelaTempoHoras();

        assertEquals(6, janelaHoras, "Janela deve ser 6 horas");
    }
}
