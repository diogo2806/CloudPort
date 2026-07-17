package br.com.cloudport.servicogate.integration.ocr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessamentoOcrRecuperacaoJobTest {

    @Mock
    private DocumentoAgendamentoRepository documentoAgendamentoRepository;

    @Mock
    private ProcessamentoOcrPublisher processamentoOcrPublisher;

    @Test
    void deveDeduplicarCandidatosEDelegarReivindicacaoAtomica() {
        DocumentoAgendamento pendente = documento(10L);
        DocumentoAgendamento expiradoComMesmoId = documento(10L);
        DocumentoAgendamento falhaElegivel = documento(20L);
        when(documentoAgendamentoRepository
                .findTop100ByStatusValidacaoOrderByUpdatedAtAsc(StatusValidacaoDocumento.PENDENTE))
                .thenReturn(Collections.singletonList(pendente));
        when(documentoAgendamentoRepository
                .findTop100ByStatusValidacaoAndProcessamentoOcrIniciadoEmBeforeOrderByProcessamentoOcrIniciadoEmAsc(
                        eq(StatusValidacaoDocumento.PROCESSANDO),
                        any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiradoComMesmoId));
        when(documentoAgendamentoRepository
                .findTop100ByStatusValidacaoAndProximaTentativaOcrEmLessThanEqualAndTentativasOcrLessThanOrderByProximaTentativaOcrEmAsc(
                        eq(StatusValidacaoDocumento.FALHA),
                        any(LocalDateTime.class),
                        eq(3)))
                .thenReturn(Collections.singletonList(falhaElegivel));

        ProcessamentoOcrRecuperacaoJob job = new ProcessamentoOcrRecuperacaoJob(
                documentoAgendamentoRepository,
                processamentoOcrPublisher,
                Duration.ofMinutes(10),
                3);

        job.recuperarProcessamentosPendentes();

        verify(processamentoOcrPublisher, times(1)).reivindicarESubmeter(10L);
        verify(processamentoOcrPublisher, times(1)).reivindicarESubmeter(20L);
    }

    private DocumentoAgendamento documento(Long id) {
        DocumentoAgendamento documento = new DocumentoAgendamento();
        documento.setId(id);
        return documento;
    }
}
