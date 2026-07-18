package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.ComandoTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.TampaResposta;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusMovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TampaPoraoComandoTransacionalServico")
class TampaPoraoComandoTransacionalServicoTest {

    private EstivagemPlanRepositorio planRepositorio;
    private ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;
    private TarefaTampaPoraoRepositorio tarefaRepositorio;
    private TampaPoraoServico tampaPoraoServico;
    private TampaPoraoComandoTransacionalServico servico;
    private EstivagemPlan plan;
    private ExecucaoSequenciaGuindaste execucao;
    private MovimentoExecucaoGuindaste movimento;
    private TarefaTampaPorao tarefa;
    private ComandoTarefaRequest request;

    @BeforeEach
    void setup() {
        planRepositorio = mock(EstivagemPlanRepositorio.class);
        execucaoRepositorio = mock(ExecucaoSequenciaGuindasteRepositorio.class);
        tarefaRepositorio = mock(TarefaTampaPoraoRepositorio.class);
        tampaPoraoServico = mock(TampaPoraoServico.class);
        servico = new TampaPoraoComandoTransacionalServico(
                planRepositorio,
                execucaoRepositorio,
                tarefaRepositorio,
                tampaPoraoServico);

        plan = new EstivagemPlan();
        plan.setId(10L);

        SlotNavio slot = new SlotNavio();
        slot.setEstivagem(plan);
        slot.setBay(1);
        slot.setRowBay(2);
        slot.setTier(3);
        slot.setCodigoHatchCover("HC-01");
        slot.setSobreHatchCover(false);
        slot.setCodigoContainer("CONT001");
        plan.getSlots().add(slot);

        TampaPorao tampa = new TampaPorao();
        tampa.setId(20L);
        tampa.setEstivagem(plan);
        tampa.setCodigo("HC-01");

        tarefa = new TarefaTampaPorao();
        tarefa.setId(30L);
        tarefa.setTampa(tampa);
        tarefa.setTipo(TipoOperacaoTampaPorao.POSICIONAR);

        execucao = new ExecucaoSequenciaGuindaste();
        execucao.setEstivagem(plan);
        movimento = new MovimentoExecucaoGuindaste();
        movimento.setBay(1);
        movimento.setRowBay(2);
        movimento.setTier(3);
        movimento.setStatus(StatusMovimentoExecucaoGuindaste.PLANEJADO);
        execucao.adicionarMovimento(movimento);

        request = new ComandoTarefaRequest();
        when(execucaoRepositorio.findLockedByEstivagemId(10L)).thenReturn(Optional.of(execucao));
        when(planRepositorio.findLockedById(10L)).thenReturn(Optional.of(plan));
        when(tarefaRepositorio.findById(30L)).thenReturn(Optional.of(tarefa));
    }

    @Test
    @DisplayName("Deve impedir tarefa enquanto houver movimento ativo sob a mesma tampa")
    void deveImpedirTarefaDuranteMovimentoAtivo() {
        tarefa.setTipo(TipoOperacaoTampaPorao.FECHAR);
        movimento.setStatus(StatusMovimentoExecucaoGuindaste.EM_EXECUCAO);

        assertThrows(
                IllegalStateException.class,
                () -> servico.iniciarTarefa(10L, 30L, request, "operador"));
    }

    @Test
    @DisplayName("Deve impedir posicionamento enquanto houver movimento pendente no porão")
    void deveImpedirPosicionamentoComMovimentoPendente() {
        assertThrows(
                IllegalStateException.class,
                () -> servico.iniciarTarefa(10L, 30L, request, "operador"));
    }

    @Test
    @DisplayName("Deve permitir posicionamento após os movimentos do porão terminarem")
    void devePermitirPosicionamentoAposMovimentosTerminais() {
        movimento.setStatus(StatusMovimentoExecucaoGuindaste.CONCLUIDO);
        TampaResposta resposta = new TampaResposta();
        when(tampaPoraoServico.iniciarTarefa(10L, 30L, request, "operador"))
                .thenReturn(resposta);

        TampaResposta resultado = servico.iniciarTarefa(10L, 30L, request, "operador");

        assertSame(resposta, resultado);
        verify(tampaPoraoServico).iniciarTarefa(10L, 30L, request, "operador");
    }
}
