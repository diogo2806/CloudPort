package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WorkQueueOperacaoEstadoRealTest {

    @Mock
    private WorkQueuePatioRepositorio workQueueRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private HistoricoWorkInstructionRepositorio historicoRepositorio;
    @Mock
    private EquipamentoPatioRepositorio equipamentoRepositorio;

    private WorkQueueOperacaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new WorkQueueOperacaoServico(
                workQueueRepositorio,
                ordemRepositorio,
                historicoRepositorio,
                equipamentoRepositorio);
    }

    @Test
    void deveRecusarDispatchSemCoberturaOperacionalCompleta() {
        WorkQueuePatio fila = filaOperacional();
        fila.setPow(null);
        when(workQueueRepositorio.findById(10L)).thenReturn(Optional.of(fila));

        assertThrows(ResponseStatusException.class, () -> servico.despachar(10L, dispatch()));
    }

    @Test
    void deveDespacharSomentePelaMatrizComCheReal() {
        WorkQueuePatio fila = filaOperacional();
        EquipamentoPatio equipamento = equipamentoOperacional();
        OrdemTrabalhoPatio ordem = ordemPendente();
        when(workQueueRepositorio.findById(10L)).thenReturn(Optional.of(fila));
        when(equipamentoRepositorio.findById(20L)).thenReturn(Optional.of(equipamento));
        when(ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(10L))
                .thenReturn(List.of(ordem));
        when(ordemRepositorio.save(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoDispatchWorkQueueDto resultado = servico.despachar(10L, dispatch());

        assertEquals(1, resultado.getTotalOrdensDespachadas());
        assertEquals(StatusOrdemTrabalhoPatio.EM_EXECUCAO, ordem.getStatusOrdem());
        verify(historicoRepositorio, atLeast(2)).save(any(HistoricoOperacaoPatio.class));
    }

    @Test
    void deveExporCoberturaRealParaValidacaoDoPlano() {
        WorkQueuePatio fila = filaOperacional();
        EquipamentoPatio equipamento = equipamentoOperacional();
        OrdemTrabalhoPatio ordem = ordemPendente();
        when(workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(30L))
                .thenReturn(List.of(fila));
        when(equipamentoRepositorio.findById(20L)).thenReturn(Optional.of(equipamento));
        when(ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(10L))
                .thenReturn(List.of(ordem));

        List<WorkQueueValidacaoPlanoDto> resultado = servico.consultarValidacaoPlano(30L);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isCoberturaValida());
        assertEquals(1, resultado.get(0).getTotalOrdensDispatchaveis());
        assertEquals("RTG-01", resultado.get(0).getEquipamentoIdentificador());
    }

    private WorkQueuePatio filaOperacional() {
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setId(10L);
        fila.setIdentificador("WQ-10");
        fila.setVisitaNavioId(30L);
        fila.setBerco("B01");
        fila.setPorao(2);
        fila.setRecursoCaisId(40L);
        fila.setPow("POW-01");
        fila.setPoolOperacional("POOL-01");
        fila.setEquipamentoPatioId(20L);
        fila.setEquipamento("RTG-01");
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        return fila;
    }

    private EquipamentoPatio equipamentoOperacional() {
        return new EquipamentoPatio(
                20L,
                "RTG-01",
                TipoEquipamento.RTG,
                1,
                1,
                StatusEquipamento.OPERACIONAL);
    }

    private OrdemTrabalhoPatio ordemPendente() {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(50L);
        ordem.setWorkQueueId(10L);
        ordem.setCodigoConteiner("CONT-50");
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.PENDENTE);
        return ordem;
    }

    private DispatchWorkQueueDto dispatch() {
        DispatchWorkQueueDto dto = new DispatchWorkQueueDto();
        dto.setMotivo("Despacho autorizado pelo planejador");
        dto.setUsuario("operador-teste");
        dto.setOrigemAcao("TESTE");
        dto.setCorrelationId("corr-123");
        return dto;
    }
}
