package br.com.cloudport.runtime.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.servico.PredictiveSchedulerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtimizacaoYardLocalAdapterTest {

    @Mock
    private PredictiveSchedulerService schedulerService;

    private OtimizacaoYardLocalAdapter adapter;

    @BeforeEach
    void configurar() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        adapter = new OtimizacaoYardLocalAdapter(schedulerService, objectMapper);
    }

    @Test
    void deveConverterContratoEInvocarSchedulerLocal() {
        LocalDateTime chegada = LocalDateTime.of(2026, 7, 16, 8, 0);
        LocalDateTime partida = chegada.plusHours(12);
        Map<String, Object> requisicao = Map.of(
                "navio", Map.of(
                        "codigoNavio", "NAV-001",
                        "nomeBerco", "B01",
                        "etaChegada", chegada,
                        "etaPartida", partida,
                        "quantidadeContainersImportacao", 1,
                        "quantidadeContainersExportacao", 0,
                        "prioridade", "PLANEJADA"),
                "equipamentosDisponiveis", List.of("RTG-01"),
                "containersImportacao", List.of(Map.of(
                        "codigoContainer", "CONT-001",
                        "linha", 1,
                        "coluna", 2)),
                "containersExportacao", List.of()
        );
        SchedulerResultDto resultado = new SchedulerResultDto("NAV-001");
        resultado.setStatusGeral("BOM");
        when(schedulerService.gerarPlanoOperacional(any(SchedulerPlanoOperacionalRequisicaoDto.class)))
                .thenReturn(resultado);

        Map<String, Object> plano = adapter.otimizar(requisicao);

        ArgumentCaptor<SchedulerPlanoOperacionalRequisicaoDto> captor =
                ArgumentCaptor.forClass(SchedulerPlanoOperacionalRequisicaoDto.class);
        verify(schedulerService).gerarPlanoOperacional(captor.capture());
        assertEquals("NAV-001", captor.getValue().getNavio().getCodigoNavio());
        assertEquals(List.of("RTG-01"), captor.getValue().getEquipamentosDisponiveis());
        assertEquals("CONT-001", captor.getValue().getContainersImportacao().get(0).getCodigoContainer());
        assertEquals("NAV-001", plano.get("codigoNavio"));
        assertEquals("BOM", plano.get("statusGeral"));
    }
}
