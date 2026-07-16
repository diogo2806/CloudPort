package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReconciliacaoNavioPatioJobTest {

    @Mock
    private ItemOperacaoNavioRepositorio itemRepositorio;
    @Mock
    private SincronizadorStatusNavioPatioServico sincronizadorStatus;
    @Mock
    private ExecucaoUnicaServico execucaoUnicaServico;

    private ReconciliacaoNavioPatioJob job;

    @BeforeEach
    void configurar() {
        job = new ReconciliacaoNavioPatioJob(
                itemRepositorio,
                sincronizadorStatus,
                execucaoUnicaServico
        );
    }

    @Test
    void naoDeveExecutarQuandoOutraInstanciaPossuiOBloqueio() {
        when(execucaoUnicaServico.executarSeDisponivel(anyString(), any(Runnable.class)))
                .thenReturn(false);

        job.reconciliarVisitasAtivas();

        verify(itemRepositorio, never())
                .findTop100ByStatusIntegracaoPatioInOrderByAtualizadoEmAsc(anyCollection());
        verify(sincronizadorStatus, never()).sincronizarStatus(any());
    }

    @Test
    void deveExecutarUmaVezEApenasParaVisitasComPendencias() {
        VisitaNavio visita42 = visita(42L);
        VisitaNavio visita43 = visita(43L);
        when(itemRepositorio.findTop100ByStatusIntegracaoPatioInOrderByAtualizadoEmAsc(anyCollection()))
                .thenReturn(List.of(item(visita42), item(visita42), item(visita43)));
        when(execucaoUnicaServico.executarSeDisponivel(anyString(), any(Runnable.class)))
                .thenAnswer(invocacao -> {
                    Runnable operacao = invocacao.getArgument(1);
                    operacao.run();
                    return true;
                });
        when(sincronizadorStatus.sincronizarStatus(42L)).thenReturn(1);
        when(sincronizadorStatus.sincronizarStatus(43L)).thenReturn(0);

        job.reconciliarVisitasAtivas();

        verify(sincronizadorStatus).sincronizarStatus(42L);
        verify(sincronizadorStatus).sincronizarStatus(43L);
    }

    private VisitaNavio visita(Long id) {
        VisitaNavio visita = new VisitaNavio();
        ReflectionTestUtils.setField(visita, "id", id);
        return visita;
    }

    private ItemOperacaoNavio item(VisitaNavio visita) {
        ItemOperacaoNavio item = new ItemOperacaoNavio();
        item.setVisitaNavio(visita);
        return item;
    }
}
