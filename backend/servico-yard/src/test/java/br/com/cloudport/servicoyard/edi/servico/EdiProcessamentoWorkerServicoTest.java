package br.com.cloudport.servicoyard.edi.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdiProcessamentoWorkerServicoTest {

    @Mock
    private ProcessamentoEdiRepositorio repositorio;

    @Mock
    private EdiProcessadorServico processador;

    @Mock
    private BayPlanRespostaDto bayPlan;

    private EdiProcessamentoWorkerServico servico;

    @BeforeEach
    void configurar() {
        servico = new EdiProcessamentoWorkerServico(repositorio, processador);
    }

    @Test
    void deveConcluirMensagemReservadaPeloWorker() {
        ProcessamentoEdi processamento = processamentoBaplie();
        when(repositorio.buscarPendentesParaProcessamento(anyCollection(), any(), any()))
                .thenReturn(List.of(processamento));
        when(processador.processarBaplie(processamento.getConteudoOriginal())).thenReturn(bayPlan);
        when(bayPlan.getId()).thenReturn(31L);
        when(bayPlan.getCodigoNavio()).thenReturn("NAV-31");
        when(bayPlan.getCodigoViagem()).thenReturn("V31");

        servico.processarProximo();

        assertEquals(StatusProcessamentoEdi.CONCLUIDO, processamento.getStatus());
        assertEquals(1, processamento.getTentativa());
        assertEquals(31L, processamento.getBayPlanId());
        verify(repositorio, times(2)).saveAndFlush(processamento);
    }

    @Test
    void devePropagarFalhaIrrecuperavelParaRegistroForaDaTransacao() {
        ProcessamentoEdi processamento = processamentoBaplie();
        when(repositorio.buscarPendentesParaProcessamento(anyCollection(), any(), any()))
                .thenReturn(List.of(processamento));
        when(processador.processarBaplie(processamento.getConteudoOriginal()))
                .thenThrow(new IllegalArgumentException("BAPLIE invalido"));

        FalhaProcessamentoEdiException erro = assertThrows(
                FalhaProcessamentoEdiException.class,
                servico::processarProximo
        );

        assertTrue(erro.isIrrecuperavel());
    }

    private ProcessamentoEdi processamentoBaplie() {
        ProcessamentoEdi processamento = new ProcessamentoEdi();
        processamento.setTipoMensagem(TipoMensagemEdi.BAPLIE);
        processamento.setStatus(StatusProcessamentoEdi.RECEBIDO);
        processamento.setConteudoOriginal("UNH+1+BAPLIE:D:95B:UN'");
        processamento.setTentativa(0);
        return processamento;
    }
}
