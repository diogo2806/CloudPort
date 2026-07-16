package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
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
    private VisitaNavioRepositorio visitaRepositorio;
    @Mock
    private SincronizadorStatusNavioPatioServico sincronizadorStatus;
    @Mock
    private ExecucaoUnicaServico execucaoUnicaServico;

    private ReconciliacaoNavioPatioJob job;

    @BeforeEach
    void configurar() {
        job = new ReconciliacaoNavioPatioJob(
                visitaRepositorio,
                sincronizadorStatus,
                execucaoUnicaServico
        );
    }

    @Test
    void naoDeveExecutarQuandoOutraInstanciaPossuiOBloqueio() {
        when(execucaoUnicaServico.executarSeDisponivel(anyString(), any(Runnable.class)))
                .thenReturn(false);

        job.reconciliarVisitasAtivas();

        verify(visitaRepositorio, never()).findAllByOrderByEtaDesc();
        verify(sincronizadorStatus, never()).sincronizarStatus(any());
    }

    @Test
    void deveExecutarUmaVezEApenasParaVisitasNaoTerminais() {
        VisitaNavio ativa = visita(42L, FaseVisitaNavio.OPERANDO);
        VisitaNavio encerrada = visita(43L, FaseVisitaNavio.PARTIU);
        when(visitaRepositorio.findAllByOrderByEtaDesc()).thenReturn(List.of(ativa, encerrada));
        when(execucaoUnicaServico.executarSeDisponivel(anyString(), any(Runnable.class)))
                .thenAnswer(invocacao -> {
                    Runnable operacao = invocacao.getArgument(1);
                    operacao.run();
                    return true;
                });
        when(sincronizadorStatus.sincronizarStatus(42L)).thenReturn(1);

        job.reconciliarVisitasAtivas();

        verify(sincronizadorStatus).sincronizarStatus(42L);
        verify(sincronizadorStatus, never()).sincronizarStatus(43L);
    }

    private VisitaNavio visita(Long id, FaseVisitaNavio fase) {
        VisitaNavio visita = new VisitaNavio();
        ReflectionTestUtils.setField(visita, "id", id);
        visita.setFase(fase);
        return visita;
    }
}
