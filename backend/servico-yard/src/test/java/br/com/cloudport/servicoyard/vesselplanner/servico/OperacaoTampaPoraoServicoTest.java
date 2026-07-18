package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.PosicaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TampaPoraoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperacaoTampaPoraoServico")
class OperacaoTampaPoraoServicoTest {

    private TampaPoraoRepositorio tampaRepositorio;
    private TarefaTampaPoraoRepositorio tarefaRepositorio;
    private OperacaoTampaPoraoServico servico;
    private EstivagemPlan plan;
    private TampaPorao tampa;

    @BeforeEach
    void setup() {
        tampaRepositorio = mock(TampaPoraoRepositorio.class);
        tarefaRepositorio = mock(TarefaTampaPoraoRepositorio.class);
        servico = new OperacaoTampaPoraoServico(
                mock(EstivagemPlanRepositorio.class),
                tampaRepositorio,
                tarefaRepositorio,
                mock(ExecucaoSequenciaGuindasteRepositorio.class));

        plan = new EstivagemPlan();
        plan.setId(10L);
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
    @DisplayName("Deve bloquear acesso ao porão com tampa fechada")
    void deveBloquearPoraoComTampaFechada() {
        tampa.setPosicao(PosicaoTampaPorao.FECHADA);

        String motivo = servico.motivoBloqueio(plan, "HC-01", false);

        assertTrue(motivo.contains("exige a remoção"));
    }

    @Test
    @DisplayName("Deve liberar acesso ao porão com tampa removida")
    void deveLiberarPoraoComTampaRemovida() {
        tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);

        assertNull(servico.motivoBloqueio(plan, "HC-01", false));
    }

    @Test
    @DisplayName("Deve bloquear slot sobre escotilha com tampa removida")
    void deveBloquearConvesComTampaRemovida() {
        tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);

        String motivo = servico.motivoBloqueio(plan, "HC-01", true);

        assertTrue(motivo.contains("posicionada ou fechada"));
    }

    @Test
    @DisplayName("Deve bloquear movimento durante tarefa em execução")
    void deveBloquearDuranteTarefaEmExecucao() {
        tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);
        TarefaTampaPorao tarefa = new TarefaTampaPorao();
        tarefa.setStatus(StatusTarefaTampaPorao.EM_EXECUCAO);
        when(tarefaRepositorio.findByTampaIdOrderByOrdemOperacionalAscIdAsc(30L))
                .thenReturn(List.of(tarefa));

        String motivo = servico.motivoBloqueio(plan, "HC-01", false);

        assertTrue(motivo.contains("está em operação"));
    }
}
