package br.com.cloudport.servicogate.integration.ocr;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessamentoOcrPublisherTest {

    @Mock
    private ProcessamentoOcrExecutor executor;

    @InjectMocks
    private ProcessamentoOcrPublisher processamentoOcrPublisher;

    @Test
    void deveExecutarOcrDiretamenteSemFila() {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(20L);

        DocumentoAgendamento documento = new DocumentoAgendamento();
        documento.setId(10L);
        documento.setAgendamento(agendamento);
        documento.setUrlDocumento("documentos/20/arquivo.png");

        processamentoOcrPublisher.enfileirarProcessamento(documento);

        verify(executor).processar(argThat(mensagem ->
                Long.valueOf(10L).equals(mensagem.getDocumentoId())
                        && Long.valueOf(20L).equals(mensagem.getAgendamentoId())
                        && "documentos/20/arquivo.png".equals(mensagem.getChaveArmazenamento())));
    }
}
