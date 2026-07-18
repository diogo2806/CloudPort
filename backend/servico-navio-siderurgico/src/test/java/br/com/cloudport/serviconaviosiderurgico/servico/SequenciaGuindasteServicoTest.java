package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.dominio.SequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusSequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandosSequenciaGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.SequenciaGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.AuditoriaSequenciaGuindasteRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.EventoOutboxSequenciaGuindasteRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.SequenciaGuindasteRepositorio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SequenciaGuindasteServicoTest {

    @Mock
    private SequenciaGuindasteRepositorio sequenciaRepositorio;
    @Mock
    private AuditoriaSequenciaGuindasteRepositorio auditoriaRepositorio;
    @Mock
    private EventoOutboxSequenciaGuindasteRepositorio outboxRepositorio;

    private SequenciaGuindasteServico servico;

    @BeforeEach
    void configurar() {
        servico = new SequenciaGuindasteServico(sequenciaRepositorio, auditoriaRepositorio, outboxRepositorio);
    }

    @Test
    void deveIniciarSequenciaPlanejadaERegistrarAuditoriaEOutbox() {
        SequenciaGuindaste sequencia = sequencia(StatusSequenciaGuindaste.PLANNED);
        when(sequenciaRepositorio.findByMovementId("mov-1")).thenReturn(Optional.of(sequencia));
        when(sequenciaRepositorio.saveAndFlush(sequencia)).thenReturn(sequencia);
        when(outboxRepositorio.existsByEventKey("mov-1:STARTED")).thenReturn(false);

        SequenciaGuindasteDTO resposta = servico.iniciar(
                "mov-1",
                new ComandosSequenciaGuindasteDTO.Transicao("operador-1", null));

        assertEquals(StatusSequenciaGuindaste.STARTED, resposta.status());
        assertEquals("operador-1", resposta.operatorId());
        assertNotNull(resposta.startedAt());
        verify(auditoriaRepositorio).save(any());
        verify(outboxRepositorio).saveAndFlush(any());
    }

    @Test
    void deveRejeitarFinalizacaoQuandoMovimentoAindaEstaPlanejado() {
        SequenciaGuindaste sequencia = sequencia(StatusSequenciaGuindaste.PLANNED);
        when(sequenciaRepositorio.findByMovementId("mov-1")).thenReturn(Optional.of(sequencia));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> servico.finalizar(
                        "mov-1",
                        new ComandosSequenciaGuindasteDTO.Transicao("operador-1", null)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        verify(sequenciaRepositorio, never()).saveAndFlush(any());
    }

    @Test
    void deveSerIdempotenteAoRepetirInicio() {
        SequenciaGuindaste sequencia = sequencia(StatusSequenciaGuindaste.STARTED);
        sequencia.setStartedAt(LocalDateTime.now());
        when(sequenciaRepositorio.findByMovementId("mov-1")).thenReturn(Optional.of(sequencia));

        SequenciaGuindasteDTO resposta = servico.iniciar(
                "mov-1",
                new ComandosSequenciaGuindasteDTO.Transicao("operador-1", null));

        assertEquals(StatusSequenciaGuindaste.STARTED, resposta.status());
        verify(sequenciaRepositorio, never()).saveAndFlush(any());
        verify(auditoriaRepositorio, never()).save(any());
        verify(outboxRepositorio, never()).saveAndFlush(any());
    }

    private SequenciaGuindaste sequencia(StatusSequenciaGuindaste status) {
        SequenciaGuindaste sequencia = new SequenciaGuindaste();
        sequencia.setMovementId("mov-1");
        sequencia.setVesselVisitId("10");
        sequencia.setCraneId("QC-01");
        sequencia.setLoadUnitId("100");
        sequencia.setPlannedStart(LocalDateTime.now().plusMinutes(30));
        sequencia.setStatus(status);
        return sequencia;
    }
}
