package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoInternoProcessado;
import br.com.cloudport.serviconaviosiderurgico.repositorio.EventoInternoProcessadoRepositorio;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessamentoEventoInternoServicoTest {

    @Mock
    private EventoInternoProcessadoRepositorio repositorio;

    @Test
    void deveExecutarEfeitoSomenteNaPrimeiraEntrega() {
        UUID eventoId = UUID.randomUUID();
        AtomicInteger execucoes = new AtomicInteger();
        when(repositorio.existsById(eventoId.toString())).thenReturn(false, true);
        ProcessamentoEventoInternoServico servico = new ProcessamentoEventoInternoServico(repositorio);

        boolean primeira = servico.processarUmaVez(eventoId, "YARD:ALTERADO", execucoes::incrementAndGet);
        boolean segunda = servico.processarUmaVez(eventoId, "YARD:ALTERADO", execucoes::incrementAndGet);

        assertThat(primeira).isTrue();
        assertThat(segunda).isFalse();
        assertThat(execucoes.get()).isEqualTo(1);
        verify(repositorio).saveAndFlush(any(EventoInternoProcessado.class));
    }

    @Test
    void naoDevePersistirNemExecutarQuandoEventoJaFoiProcessado() {
        UUID eventoId = UUID.randomUUID();
        AtomicInteger execucoes = new AtomicInteger();
        when(repositorio.existsById(eventoId.toString())).thenReturn(true);
        ProcessamentoEventoInternoServico servico = new ProcessamentoEventoInternoServico(repositorio);

        boolean processado = servico.processarUmaVez(eventoId, "NAVIO_CANONICO:ATUALIZADO",
                execucoes::incrementAndGet);

        assertThat(processado).isFalse();
        assertThat(execucoes.get()).isZero();
        verify(repositorio, never()).saveAndFlush(any());
    }
}
