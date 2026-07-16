package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkQueueValidacaoPlanoServicoTest {

    @Mock
    private WorkQueuePatioRepositorio workQueueRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private EquipamentoPatioRepositorio equipamentoRepositorio;
    @Mock
    private WorkQueueOperacaoServico operacaoServico;

    private WorkQueueValidacaoPlanoServico servico;

    @BeforeEach
    void configurar() {
        servico = new WorkQueueValidacaoPlanoServico(
                workQueueRepositorio,
                ordemRepositorio,
                equipamentoRepositorio,
                operacaoServico);
    }

    @Test
    void deveConfirmarCoberturaSomenteComRecursosReaisEJobDispatchavel() {
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setId(10L);
        fila.setVisitaNavioId(30L);
        fila.setIdentificador("WQ-10");
        fila.setBerco("B01");
        fila.setPorao(2);
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setPow("POW-01");
        fila.setPoolOperacional("POOL-01");
        fila.setEquipamentoPatioId(20L);
        fila.setRecursoCaisId(40L);

        EquipamentoPatio equipamento = new EquipamentoPatio(
                20L,
                "RTG-01",
                TipoEquipamento.RTG,
                1,
                1,
                StatusEquipamento.OPERACIONAL);
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(50L);
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.PENDENTE);

        when(workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(30L))
                .thenReturn(List.of(fila));
        when(equipamentoRepositorio.findById(20L)).thenReturn(Optional.of(equipamento));
        when(ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(10L))
                .thenReturn(List.of(ordem));
        when(operacaoServico.matrizOficialEstados())
                .thenReturn(Map.of("PENDENTE", List.of("EM_EXECUCAO", "SUSPENSA")));

        List<WorkQueueValidacaoPlanoDto> resultado = servico.consultar(30L);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isCoberturaValida());
        assertEquals("RTG-01", resultado.get(0).getEquipamentoIdentificador());
        assertEquals(1, resultado.get(0).getTotalOrdensDispatchaveis());
    }
}
