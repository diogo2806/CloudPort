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
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    void deveExporMatrizOficialDeEstados() {
        Map<String, List<String>> matriz = servico.matrizOficialEstados();

        assertTrue(matriz.get("PENDENTE").contains("EM_EXECUCAO"));
        assertTrue(matriz.get("EM_EXECUCAO").contains("PENDENTE"));
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

        servico.suspender(10L, comando("Interdição operacional"));

        assertEquals(StatusOrdemTrabalhoPatio.SUSPENSA, ordem.getStatusOrdem());
        verify(historicoRepositorio).save(any(HistoricoOperacaoPatio.class));
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

        AtualizacaoPrioridadesWorkInstructionDto comando = new AtualizacaoPrioridadesWorkInstructionDto();
        comando.setPrioridadeOperacional(3);
        comando.setPrioridadeBusca(true);
        comando.setMotivo("Antecipação do equipamento");
        comando.setUsuario("operador-teste");

        servico.atualizarPrioridades(12L, comando);

        assertEquals(3, ordem.getPrioridadeOperacional());
        assertTrue(ordem.isPrioridadeBusca());
        verify(historicoRepositorio, times(2)).save(any(HistoricoOperacaoPatio.class));
    }

    @Test
    void deveBloquearDispatchSemCoberturaReal() {
        WorkQueuePatio fila = filaOperacional(20L);
        fila.setEquipamentoPatioId(null);
        fila.setEquipamento("RTG-TEXTO");
        when(workQueueRepositorio.findById(20L)).thenReturn(Optional.of(fila));

        DispatchWorkQueueDto comando = dispatch("Cobertura insuficiente");

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
                () -> servico.despachar(20L, comando));

        assertTrue(excecao.getReason().contains("CHE real"));
    }

    @Test
    void deveDespacharComCoberturaRealPelaMatrizOficial() {
        WorkQueuePatio fila = filaOperacional(21L);
        EquipamentoPatio equipamento = new EquipamentoPatio(
                9L,
                "RTG-09",
                TipoEquipamento.RTG,
                1,
                1,
                StatusEquipamento.OPERACIONAL);
        OrdemTrabalhoPatio primeira = ordem(101L, StatusOrdemTrabalhoPatio.PENDENTE);
        OrdemTrabalhoPatio segunda = ordem(102L, StatusOrdemTrabalhoPatio.BLOQUEADA);
        when(workQueueRepositorio.findById(21L)).thenReturn(Optional.of(fila));
        when(equipamentoRepositorio.findById(9L)).thenReturn(Optional.of(equipamento));
        when(ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(21L))
                .thenReturn(List.of(primeira, segunda));
        when(ordemRepositorio.save(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoDispatchWorkQueueDto resultado = servico.despachar(21L, dispatch("Janela operacional"));

        assertEquals(1, resultado.getTotalOrdensDespachadas());
        assertEquals(1, resultado.getTotalOrdensIgnoradas());
        assertEquals(StatusOrdemTrabalhoPatio.EM_EXECUCAO, primeira.getStatusOrdem());
        assertEquals(StatusOrdemTrabalhoPatio.BLOQUEADA, segunda.getStatusOrdem());
        verify(historicoRepositorio, times(2)).save(any(HistoricoOperacaoPatio.class));
    }

    private WorkQueuePatio filaOperacional(Long id) {
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setId(id);
        fila.setIdentificador("WQ-" + id);
        fila.setVisitaNavioId(42L);
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setPow("POW-1");
        fila.setPoolOperacional("POOL-1");
        fila.setPlanoGuindasteId(31L);
        fila.setRecursoCaisId(44L);
        fila.setEquipamentoPatioId(9L);
        fila.setEquipamento("RTG-09");
        fila.setCriadoEm(LocalDateTime.now());
        fila.setAtualizadoEm(LocalDateTime.now());
        return fila;
    }

    private OrdemTrabalhoPatio ordem(Long id, StatusOrdemTrabalhoPatio status) {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(id);
        ordem.setWorkQueueId(id >= 100 ? 21L : 99L);
        ordem.setCodigoConteiner("CONT-" + id);
        ordem.setStatusOrdem(status);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());
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

    private DispatchWorkQueueDto dispatch(String motivo) {
        DispatchWorkQueueDto comando = new DispatchWorkQueueDto();
        comando.setMotivo(motivo);
        comando.setOperador("operador-teste");
        comando.setOrigemAcao("TESTE");
        comando.setCorrelationId("corr-dispatch");
        return comando;
    }
}
