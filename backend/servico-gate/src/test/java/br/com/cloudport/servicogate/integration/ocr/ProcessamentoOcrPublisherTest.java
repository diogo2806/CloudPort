package br.com.cloudport.servicogate.integration.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessamentoOcrPublisherTest {

    @Mock
    private ProcessamentoOcrExecutor processamentoOcrExecutor;

    @Mock
    private DocumentoAgendamentoRepository documentoAgendamentoRepository;

    @Mock
    private Executor ocrTaskExecutor;

    @Test
    void deveSubmeterOcrAoExecutorSeparado() {
        ProcessamentoOcrPublisher publisher = new ProcessamentoOcrPublisher(
                processamentoOcrExecutor,
                documentoAgendamentoRepository,
                ocrTaskExecutor,
                Duration.ofMinutes(10),
                3);
        Agendamento agendamento = new Agendamento();
        agendamento.setId(20L);
        DocumentoAgendamento documento = new DocumentoAgendamento();
        documento.setId(10L);
        documento.setAgendamento(agendamento);
        documento.setUrlDocumento("documentos/20/arquivo.png");

        publisher.enfileirarProcessamento(documento);

        assertThat(documento.getStatusValidacao()).isEqualTo(StatusValidacaoDocumento.PENDENTE);
        verify(ocrTaskExecutor).execute(any(Runnable.class));
    }
}
