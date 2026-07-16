package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoOperacaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkQueuePatioServicoTest {

    @Mock
    private WorkQueuePatioRepositorio workQueueRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private HistoricoOperacaoPatioRepositorio historicoRepositorio;

    private WorkQueuePatioServico servico;
    private WorkQueuePatio fila;

    @BeforeEach
    void configurar() {
        servico = new WorkQueuePatioServico(workQueueRepositorio, ordemRepositorio, historicoRepositorio);
        fila = new WorkQueuePatio();
        fila.setId(9L);
        fila.setIdentificador("WQ-VISITA-42");
        fila.setVisitaNavioId(42L);
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setCriadoEm(LocalDateTime.now());
        fila.setAtualizadoEm(LocalDateTime.now());
        when(workQueueRepositorio.findById(9L)).thenReturn(Optional.of(fila));
        when(ordemRepositorio.save(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(ordemRepositorio.saveAll(anyList()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(historicoRepositorio.save(any(HistoricoOperacaoPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveRespeitarLimiteDeDispatchEAuditarFilaEInstrucoes() {
        OrdemTrabalhoPatio primeira = ordem(101L, StatusOrdemTrabalhoPatio.PENDENTE, 1);
        OrdemTrabalhoPatio segunda = ordem(102L, StatusOrdemTrabalhoPatio.PENDENTE, 2);
        OrdemTrabalhoPatio terceira = ordem(103L, StatusOrdemTrabalhoPatio.PENDENTE, 3);
        when(ordemRepositorio.findByWorkQueueIdOrderByPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(9L))
                .thenReturn(List.of(primeira, segunda, terceira));
        DispatchWorkQueueDto comando = new DispatchWorkQueueDto();
        comando.setLimiteOrdens(2);
        comando.setOperador("operador-teste");
        comando.setObservacao("janela operacional");

        ResultadoDispatchWorkQueueDto resultado = servico.despachar(9L, comando);

        assertThat(resultado.getTotalOrdensDespachadas()).isEqualTo(2);
        assertThat(resultado.getTotalOrdensIgnoradas()).isEqualTo(1);
        assertThat(primeira.getStatusOrdem()).isEqualTo(StatusOrdemTrabalhoPatio.EM_EXECUCAO);
        assertThat(segunda.getStatusOrdem()).isEqualTo(StatusOrdemTrabalhoPatio.EM_EXECUCAO);
        assertThat(terceira.getStatusOrdem()).isEqualTo(StatusOrdemTrabalhoPatio.PENDENTE);

        ArgumentCaptor<HistoricoOperacaoPatio> captor = ArgumentCaptor.forClass(HistoricoOperacaoPatio.class);
        verify(historicoRepositorio, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(HistoricoOperacaoPatio::getAcao)
                .contains("WORK_INSTRUCTION_DESPACHADA", "WORK_QUEUE_DESPACHADA");
        assertThat(captor.getAllValues())
                .allMatch(historico -> "operador-teste".equals(historico.getUsuario()));
    }

    @Test
    void deveVincularSomenteOrdensDaMesmaVisitaEAuditarVinculo() {
        OrdemTrabalhoPatio ordem = ordem(201L, StatusOrdemTrabalhoPatio.PENDENTE, 1);
        ordem.setVisitaNavioId(42L);
        when(ordemRepositorio.findByWorkQueueIdOrderByPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(9L))
                .thenReturn(List.of());
        when(ordemRepositorio.findById(201L)).thenReturn(Optional.of(ordem));

        servico.atualizarOrdens(9L, List.of(201L));

        assertThat(ordem.getWorkQueueId()).isEqualTo(9L);
        verify(ordemRepositorio).save(ordem);
        ArgumentCaptor<HistoricoOperacaoPatio> captor = ArgumentCaptor.forClass(HistoricoOperacaoPatio.class);
        verify(historicoRepositorio, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(HistoricoOperacaoPatio::getAcao)
                .contains("WORK_INSTRUCTION_VINCULADA", "JOB_LIST_ATUALIZADA");
    }

    private OrdemTrabalhoPatio ordem(Long id, StatusOrdemTrabalhoPatio status, int sequencia) {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(id);
        ordem.setCodigoConteiner("CARGA-" + id);
        ordem.setDestino("BLOCO-A");
        ordem.setLinhaDestino(1);
        ordem.setColunaDestino(sequencia);
        ordem.setCamadaDestino("1");
        ordem.setStatusOrdem(status);
        ordem.setVisitaNavioId(42L);
        ordem.setSequenciaNavio(sequencia);
        ordem.setPrioridadeOperacional(sequencia);
        ordem.setWorkQueueId(9L);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return ordem;
    }
}
