package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto.ItemPlanoOtimizadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.AplicacaoPlanoOtimizadoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusAplicacaoPlanoOtimizadoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.AplicacaoPlanoOtimizadoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

class PlanoOtimizadoPatioServicoTest {

    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private WorkQueuePatioRepositorio workQueueRepositorio;
    private PosicaoPatioRepositorio posicaoRepositorio;
    private HistoricoWorkInstructionRepositorio historicoRepositorio;
    private AplicacaoPlanoOtimizadoPatioRepositorio aplicacaoRepositorio;
    private ObjectMapper objectMapper;
    private PlanoOtimizadoPatioServico servico;

    @BeforeEach
    void configurar() {
        ordemRepositorio = mock(OrdemTrabalhoPatioRepositorio.class);
        workQueueRepositorio = mock(WorkQueuePatioRepositorio.class);
        posicaoRepositorio = mock(PosicaoPatioRepositorio.class);
        historicoRepositorio = mock(HistoricoWorkInstructionRepositorio.class);
        aplicacaoRepositorio = mock(AplicacaoPlanoOtimizadoPatioRepositorio.class);
        objectMapper = new ObjectMapper();

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);

        servico = new PlanoOtimizadoPatioServico(
                ordemRepositorio,
                workQueueRepositorio,
                posicaoRepositorio,
                historicoRepositorio,
                aplicacaoRepositorio,
                objectMapper,
                transactionManager);
    }

    @Test
    void deveRejeitarWorkQueueDeOutroBloco() {
        AplicacaoPlanoOtimizadoPatioDto comando = comando();
        when(aplicacaoRepositorio.findByPlanoIdAndVisitaNavioId("PLANO-1", 42L))
                .thenReturn(Optional.empty());
        when(aplicacaoRepositorio.saveAndFlush(any(AplicacaoPlanoOtimizadoPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setId(9L);
        fila.setVisitaNavioId(42L);
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setEquipamentoPatioId(5L);
        fila.setEquipamento("RTG-01");
        fila.setPow("POW-01");
        fila.setPoolOperacional("POOL-01");
        fila.setBlocoZona("BLOCO-B");
        fila.setCriadoEm(LocalDateTime.now());
        fila.setAtualizadoEm(LocalDateTime.now());
        when(workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(42L))
                .thenReturn(List.of(fila));

        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(100L);
        ordem.setVisitaNavioId(42L);
        ordem.setItemOperacaoNavioId(200L);
        ordem.setCodigoConteiner("CARGA-1");
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.PENDENTE);
        when(ordemRepositorio.findById(100L)).thenReturn(Optional.of(ordem));

        PosicaoPatio posicao = new PosicaoPatio(300L, 1, 2, "1");
        posicao.setBloco("BLOCO-A");
        posicao.setAreaPermitida(true);
        when(posicaoRepositorio.findByLinhaAndColunaAndCamadaOperacional(1, 2, "1"))
                .thenReturn(Optional.of(posicao));

        assertThatThrownBy(() -> servico.aplicar(comando))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("BLOCO-A");

        verify(ordemRepositorio, never()).save(any(OrdemTrabalhoPatio.class));
        verify(historicoRepositorio, never()).save(any(HistoricoOperacaoPatio.class));
    }

    @Test
    void deveRetornarResultadoConcluidoSemReaplicarAlteracoes() throws Exception {
        AplicacaoPlanoOtimizadoPatioDto comando = comando();
        ResultadoAplicacaoPlanoOtimizadoPatioDto resultadoPersistido =
                new ResultadoAplicacaoPlanoOtimizadoPatioDto();
        resultadoPersistido.setPlanoId("PLANO-1");
        resultadoPersistido.setVisitaNavioId(42L);
        resultadoPersistido.setOrdensAtualizadas(1);

        AplicacaoPlanoOtimizadoPatio aplicacao = new AplicacaoPlanoOtimizadoPatio();
        aplicacao.setPlanoId("PLANO-1");
        aplicacao.setVisitaNavioId(42L);
        aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoPatio.CONCLUIDA);
        aplicacao.setResultadoJson(objectMapper.writeValueAsString(resultadoPersistido));
        aplicacao.setCriadoEm(LocalDateTime.now());
        aplicacao.setAtualizadoEm(LocalDateTime.now());
        when(aplicacaoRepositorio.findByPlanoIdAndVisitaNavioId("PLANO-1", 42L))
                .thenReturn(Optional.of(aplicacao));

        ResultadoAplicacaoPlanoOtimizadoPatioDto resultado = servico.aplicar(comando);

        assertThat(resultado.getPlanoId()).isEqualTo("PLANO-1");
        assertThat(resultado.getOrdensAtualizadas()).isEqualTo(1);
        verifyNoInteractions(ordemRepositorio, workQueueRepositorio, posicaoRepositorio, historicoRepositorio);
    }

    private AplicacaoPlanoOtimizadoPatioDto comando() {
        ItemPlanoOtimizadoDto item = new ItemPlanoOtimizadoDto();
        item.setOrdemTrabalhoPatioId(100L);
        item.setItemOperacaoNavioId(200L);
        item.setCodigoConteiner("CARGA-1");
        item.setLinha(1);
        item.setColuna(2);
        item.setCamada("1");
        item.setEquipamento("RTG-01");
        item.setPrioridadeOperacional(1);

        AplicacaoPlanoOtimizadoPatioDto comando = new AplicacaoPlanoOtimizadoPatioDto();
        comando.setPlanoId("PLANO-1");
        comando.setVisitaNavioId(42L);
        comando.setUsuario("operador");
        comando.setItens(List.of(item));
        return comando;
    }
}
