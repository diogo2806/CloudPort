package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MomentoSequenciaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusMovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("MarcoSequenciaTampaPoraoValidador")
class MarcoSequenciaTampaPoraoValidadorTest {

    private TarefaTampaPoraoRepositorio tarefaRepositorio;
    private ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;
    private MarcoSequenciaTampaPoraoValidador validador;
    private TarefaTampaPorao tarefa;
    private ExecucaoSequenciaGuindaste execucao;
    private MovimentoExecucaoGuindaste movimento;

    @BeforeEach
    void setup() {
        tarefaRepositorio = mock(TarefaTampaPoraoRepositorio.class);
        execucaoRepositorio = mock(ExecucaoSequenciaGuindasteRepositorio.class);
        validador = new MarcoSequenciaTampaPoraoValidador(tarefaRepositorio, execucaoRepositorio);

        EstivagemPlan plan = new EstivagemPlan();
        plan.setId(10L);
        TampaPorao tampa = new TampaPorao();
        tampa.setEstivagem(plan);
        tampa.setCodigo("HC-01");
        tarefa = new TarefaTampaPorao();
        tarefa.setId(20L);
        tarefa.setTampa(tampa);
        tarefa.setTipo(TipoTarefaTampaPorao.POSICIONAR);
        tarefa.setMomentoSequencia(MomentoSequenciaTampaPorao.APOS);
        tarefa.setOrdemMovimentoReferencia(3);

        execucao = new ExecucaoSequenciaGuindaste();
        execucao.setEstivagem(plan);
        movimento = new MovimentoExecucaoGuindaste();
        movimento.setCodigoHatchCover("HC-01");
        movimento.setSobreHatchCover(false);
        movimento.setOrdemPlanejada(3);
        movimento.setStatus(StatusMovimentoExecucaoGuindaste.PLANEJADO);
        execucao.adicionarMovimento(movimento);

        when(tarefaRepositorio.findById(20L)).thenReturn(Optional.of(tarefa));
        when(execucaoRepositorio.findByEstivagemId(10L)).thenReturn(Optional.of(execucao));
    }

    @Test
    @DisplayName("Deve impedir posicionamento enquanto houver movimento pendente no porão")
    void deveImpedirPosicionamentoComMovimentoPendente() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validador.validarInicio(10L, 20L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    @DisplayName("Deve permitir posicionamento após finalizar os movimentos do porão")
    void devePermitirPosicionamentoAposMovimentosTerminais() {
        movimento.setStatus(StatusMovimentoExecucaoGuindaste.CONCLUIDO);

        assertDoesNotThrow(() -> validador.validarInicio(10L, 20L));
    }
}
