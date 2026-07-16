package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.entity.StatusNavio;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusNavioServiceTest {

    @Mock
    private StatusNavioRepository statusNavioRepository;

    @Mock
    private AlertaRepository alertaRepository;

    @Test
    void devePreservarStatusQuandoEventoAtualizaSomenteBerco() {
        StatusNavio existente = new StatusNavio();
        existente.setNavioId("NAV-1");
        existente.setStatusOperacional("operando");
        when(statusNavioRepository.findByNavioId("NAV-1")).thenReturn(Optional.of(existente));
        StatusNavioService service = new StatusNavioService(statusNavioRepository, alertaRepository);

        service.atualizarStatusNavio("NAV-1", null, "B-04");

        ArgumentCaptor<StatusNavio> captor = ArgumentCaptor.forClass(StatusNavio.class);
        verify(statusNavioRepository).save(captor.capture());
        assertEquals("operando", captor.getValue().getStatusOperacional());
        assertEquals("B-04", captor.getValue().getBercoAlocado());
        assertNotNull(captor.getValue().getDataAtualizacao());
    }

    @Test
    void deveCriarProjecaoQuandoEventoChegaAntesDoCadastro() {
        when(statusNavioRepository.findByNavioId("NAV-2")).thenReturn(Optional.empty());
        StatusNavioService service = new StatusNavioService(statusNavioRepository, alertaRepository);

        service.atualizarStatusNavio("NAV-2", "ancorando", null);

        ArgumentCaptor<StatusNavio> captor = ArgumentCaptor.forClass(StatusNavio.class);
        verify(statusNavioRepository).save(captor.capture());
        assertEquals("NAV-2", captor.getValue().getNavioId());
        assertEquals("ancorando", captor.getValue().getStatusOperacional());
        assertNotNull(captor.getValue().getChegadaReal());
    }
}
