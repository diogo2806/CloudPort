package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.PosicaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.MovimentoContainerNavioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TampaPoraoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperacaoTampaPoraoServico - bloqueio transacional de movimentos")
class OperacaoTampaPoraoServicoTest {

    private TampaPoraoRepositorio tampaRepositorio;
    private TarefaTampaPoraoRepositorio tarefaRepositorio;
    private OperacaoTampaPoraoServico servico;
    private EstivagemPlan plan;
    private SlotNavio slot;
    private TampaPorao tampa;

    @BeforeEach
    void setup() {
        tampaRepositorio = mock(TampaPoraoRepositorio.class);
        tarefaRepositorio = mock(TarefaTampaPoraoRepositorio.class);
        servico = new OperacaoTampaPoraoServico(
                mock(EstivagemPlanRepositorio.class),
                mock(SlotNavioRepositorio.class),
                tampaRepositorio,
                tarefaRepositorio,
                mock(MovimentoContainerNavioRepositorio.class));

        plan = new EstivagemPlan();
        plan.setId(10L);

        slot = new SlotNavio();
        slot.setId(20L);
        slot.setEstivagem(plan);
        slot.setCodigoHatchCover("HC-01");
        slot.setCodigoContainer("CONT001");

        tampa = new TampaPorao();
        tampa.setId(30L);
        tampa.setEstivagem(plan);
        tampa.setCodigo("HC-01");

        when(tampaRepositorio.findByEstivagemIdAndCodigo(10L, "HC-01"))
                .thenReturn(Optional.of(tampa));
        when(tarefaRepositorio.findByTampaIdOrderByOrdemOperacionalAscIdAsc(30L))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Deve bloquear acesso ao porão enquanto a tampa não estiver removida")
    void deveBloquearPoraoComTampaFechada() {
        tampa.setPosicao(PosicaoTampaPorao.FECHADA);
        slot.setSobreHatchCover(false);

        String motivo = servico.motivoBloqueio(plan, slot);

        assertTrue(motivo.contains("exige a remoção"));
    }

    @Test
    @DisplayName("Deve liberar acesso ao porão após remoção confirmada")
    void deveLiberarPoraoComTampaRemovida() {
        tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);
        slot.setSobreHatchCover(false);

        assertNull(servico.motivoBloqueio(plan, slot));
    }

    @Test
    @DisplayName("Deve bloquear contêiner sobre a escotilha quando a tampa estiver removida")
    void deveBloquearConvesComTampaRemovida() {
        tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);
        slot.setSobreHatchCover(true);

        String motivo = servico.motivoBloqueio(plan, slot);

        assertTrue(motivo.contains("posicionada ou fechada"));
    }

    @Test
    @DisplayName("Deve bloquear qualquer movimento enquanto uma tarefa da tampa estiver em execução")
    void deveBloquearDuranteTarefaEmExecucao() {
        tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);
        slot.setSobreHatchCover(false);
        TarefaTampaPorao tarefa = new TarefaTampaPorao();
        tarefa.setStatus(StatusTarefaTampaPorao.EM_EXECUCAO);
        when(tarefaRepositorio.findByTampaIdOrderByOrdemOperacionalAscIdAsc(30L))
                .thenReturn(List.of(tarefa));

        String motivo = servico.motivoBloqueio(plan, slot);

        assertTrue(motivo.contains("está em operação"));
    }
}
