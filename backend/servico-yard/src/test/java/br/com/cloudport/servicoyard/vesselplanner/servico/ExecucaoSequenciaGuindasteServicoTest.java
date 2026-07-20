package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.IniciarMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("ExecucaoSequenciaGuindasteServico")
class ExecucaoSequenciaGuindasteServicoTest {

    @Test
    @DisplayName("Deve rejeitar comando baseado em versão antiga")
    void deveRejeitarVersaoAntiga() {
        EstivagemPlanRepositorio planRepositorio = mock(EstivagemPlanRepositorio.class);
        ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio = mock(ExecucaoSequenciaGuindasteRepositorio.class);
        SequenciamentoGuindasteServico sequenciamentoServico = mock(SequenciamentoGuindasteServico.class);
        TampaPoraoServico tampaPoraoServico = mock(TampaPoraoServico.class);
        EventoOperacionalGuindasteServico eventoOperacionalServico =
                mock(EventoOperacionalGuindasteServico.class);
        ExecucaoSequenciaGuindasteServico servico = new ExecucaoSequenciaGuindasteServico(
                planRepositorio,
                execucaoRepositorio,
                sequenciamentoServico,
                tampaPoraoServico,
                eventoOperacionalServico);

        ExecucaoSequenciaGuindaste execucao = new ExecucaoSequenciaGuindaste();
        execucao.setId(15L);
        MovimentoExecucaoGuindaste movimento = new MovimentoExecucaoGuindaste();
        movimento.setId(30L);
        movimento.setVersao(4L);
        execucao.adicionarMovimento(movimento);
        when(execucaoRepositorio.findById(15L)).thenReturn(Optional.of(execucao));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> servico.iniciar(
                15L,
                30L,
                new IniciarMovimentoRequest(3L, null),
                "operador-01"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }
}
