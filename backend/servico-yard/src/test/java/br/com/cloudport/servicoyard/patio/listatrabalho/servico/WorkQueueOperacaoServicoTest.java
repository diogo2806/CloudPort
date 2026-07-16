package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadesWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WorkQueueOperacaoServicoTest {

    @Mock
    private WorkQueuePatioRepositorio workQueueRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private HistoricoWorkInstructionRepositorio historicoRepositorio;
    @Mock
    private EquipamentoPatioRepositorio equipamentoRepositorio;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private WorkQueueOperacaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new WorkQueueOperacaoServico(
                workQueueRepositorio,
                ordemRepositorio,
                historicoRepositorio,
                equipamentoRepositorio,
                eventPublisher);
    }

    @Test
    void deveExporMatrizOficialDeEstados() {
        Map<String, java.util.List<String>> matriz = servico.matrizOficialEstados();

        assertTrue(matriz.get("PENDENTE").contains("EM_EXECUCAO"));
        assertTrue(matriz.get("EM_EXECUCAO").contains("CONCLUIDA"));
        assertTrue(matriz.get("SUSPENSA").contains("PENDENTE"));
        assertTrue(matriz.get("CONCLUIDA").isEmpty());
        assertTrue(matriz.get("CANCELADA").isEmpty());
    }

    @Test
    void deveSuspenderEAuditarWorkInstruction() {
        OrdemTrabalhoPatio ordem = ordem(10L, StatusOrdemTrabalhoPatio.PENDENTE);
        when(ordemRepositorio.findById(10L)).thenReturn(Optional.of(ordem));
        when(ordemRepositorio.save(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workQueueRepositorio.findById(99L)).thenReturn(Optional.of(fila()));

        servico.suspender(10L, comando("Interdição operacional"));

        assertEquals(StatusOrdemTrabalhoPatio.SUSPENSA, ordem.getStatusOrdem());
        verify(historicoRepositorio).save(any(HistoricoOperacaoPatio.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void deveImpedirTransicaoNaoPermitida() {
        OrdemTrabalhoPatio ordem = ordem(11L, StatusOrdemTrabalhoPatio.CONCLUIDA);
        when(ordemRepositorio.findById(11L)).thenReturn(Optional.of(ordem));

        assertThrows(ResponseStatusException.class,
                () -> servico.suspender(11L, comando("Tentativa inválida")));
    }

    @Test
    void deveSepararEAuditarPrioridadeOperacionalEDefetch() {
        OrdemTrabalhoPatio ordem = ordem(12L, StatusOrdemTrabalhoPatio.PENDENTE);
        ordem.setPrioridadeOperacional(20);
        ordem.setPrioridadeBusca(false);
        when(ordemRepositorio.findById(12L)).thenReturn(Optional.of(ordem));
        when(ordemRepositorio.save(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workQueueRepositorio.findById(99L)).thenReturn(Optional.of(fila()));

        AtualizacaoPrioridadesWorkInstructionDto comando = new AtualizacaoPrioridadesWorkInstructionDto();
        comando.setPrioridadeOperacional(3);
        comando.setPrioridadeBusca(true);
        comando.setMotivo("Antecipação do equipamento");
        comando.setUsuario("operador-teste");

        servico.atualizarPrioridades(12L, comando);

        assertEquals(3, ordem.getPrioridadeOperacional());
        assertTrue(ordem.isPrioridadeBusca());
        verify(historicoRepositorio, times(2)).save(any(HistoricoOperacaoPatio.class));
        verify(eventPublisher).publishEvent(any());
    }

    private WorkQueuePatio fila() {
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setId(99L);
        fila.setVisitaNavioId(42L);
        return fila;
    }

    private OrdemTrabalhoPatio ordem(Long id, StatusOrdemTrabalhoPatio status) {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(id);
        ordem.setWorkQueueId(99L);
        ordem.setCodigoConteiner("CONT-" + id);
        ordem.setStatusOrdem(status);
        return ordem;
    }

    private ComandoWorkInstructionDto comando(String motivo) {
        ComandoWorkInstructionDto comando = new ComandoWorkInstructionDto();
        comando.setMotivo(motivo);
        comando.setUsuario("operador-teste");
        comando.setOrigemAcao("TESTE");
        comando.setCorrelationId("corr-123");
        return comando;
    }
}
