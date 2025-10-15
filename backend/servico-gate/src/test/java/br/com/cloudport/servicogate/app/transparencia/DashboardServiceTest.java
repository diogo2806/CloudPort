package br.com.cloudport.servicogate.app.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.transparencia.dto.DashboardFiltroDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRepository;
import br.com.cloudport.servicogate.app.transparencia.DashboardMetricsProjection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(agendamentoRepository);
    }

    @Test
    @DisplayName("Deve calcular ocupação por hora respeitando capacidade e cancelamentos")
    void deveCalcularOcupacaoPorHora() {
        DashboardMetricsProjection projection = Mockito.mock(DashboardMetricsProjection.class);
        when(projection.getTotalAgendamentos()).thenReturn(2L);
        when(projection.getPontuais()).thenReturn(2L);
        when(projection.getNoShow()).thenReturn(0L);
        when(projection.getOcupacaoSlots()).thenReturn(0.5D);
        when(agendamentoRepository.calcularMetricasDashboard(any(), any(), any(), any(), any()))
                .thenReturn(projection);

        JanelaAtendimento janela = new JanelaAtendimento();
        janela.setCapacidade(3);
        janela.setData(LocalDate.now().plusDays(1));
        janela.setHoraInicio(LocalTime.of(10, 0));
        janela.setHoraFim(LocalTime.of(12, 0));

        Agendamento confirmado = new Agendamento();
        confirmado.setStatus(StatusAgendamento.CONFIRMADO);
        confirmado.setJanelaAtendimento(janela);

        Agendamento cancelado = new Agendamento();
        cancelado.setStatus(StatusAgendamento.CANCELADO);
        cancelado.setJanelaAtendimento(janela);

        when(agendamentoRepository.buscarRelatorio(any(), any(), any(), any()))
                .thenReturn(List.of(confirmado, cancelado));

        var resumo = dashboardService.obterResumo(new DashboardFiltroDTO());

        assertThat(resumo.getOcupacaoPorHora()).hasSize(1);
        assertThat(resumo.getOcupacaoPorHora().get(0).getTotalAgendamentos()).isEqualTo(1L);
        assertThat(resumo.getOcupacaoPorHora().get(0).getCapacidadeSlot()).isEqualTo(3);
        assertThat(resumo.getPercentualOcupacaoSlots()).isEqualTo(50.0d);
        assertThat(resumo.getPercentualPontualidade()).isEqualTo(100.0d);
    }
}

