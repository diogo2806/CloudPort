package br.com.cloudport.servicogate.integration.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessamentoOcrPublisherTest {

    @Mock
    private ProcessamentoOcrExecutor processamentoOcrExecutor;

    @Mock
    private ProcessamentoOcrReivindicacaoService reivindicacaoService;

    @Mock
    private Executor ocrTaskExecutor;

    @Test
    void deveReivindicarESubmeterOcrAoExecutorSeparado() {
        ProcessamentoOcrPublisher publisher = novoPublisher();
        DocumentoAgendamento documento = novoDocumento();
        ProcessamentoOcrMensagem mensagem = novaMensagemReivindicada();
        when(reivindicacaoService.reivindicar(10L)).thenReturn(Optional.of(mensagem));
        ArgumentCaptor<Runnable> tarefaCaptor = ArgumentCaptor.forClass(Runnable.class);

        publisher.enfileirarProcessamento(documento);

        assertThat(documento.getStatusValidacao()).isEqualTo(StatusValidacaoDocumento.PENDENTE);
        verify(reivindicacaoService).reivindicar(10L);
        verify(ocrTaskExecutor).execute(tarefaCaptor.capture());
        tarefaCaptor.getValue().run();
        verify(processamentoOcrExecutor).processar(mensagem);
    }

    @Test
    void deveLiberarReivindicacaoQuandoExecutorRejeitarSubmissao() {
        ProcessamentoOcrPublisher publisher = novoPublisher();
        ProcessamentoOcrMensagem mensagem = novaMensagemReivindicada();
        when(reivindicacaoService.reivindicar(10L)).thenReturn(Optional.of(mensagem));
        doThrow(new RejectedExecutionException("fila cheia"))
                .when(ocrTaskExecutor)
                .execute(any(Runnable.class));

        boolean submetido = publisher.reivindicarESubmeter(10L);

        assertThat(submetido).isFalse();
        verify(reivindicacaoService).liberarReivindicacao(
                eq(mensagem),
                contains("Executor OCR indisponível"));
    }

    private ProcessamentoOcrPublisher novoPublisher() {
        return new ProcessamentoOcrPublisher(
                processamentoOcrExecutor,
                reivindicacaoService,
                ocrTaskExecutor);
    }

    private DocumentoAgendamento novoDocumento() {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(20L);
        DocumentoAgendamento documento = new DocumentoAgendamento();
        documento.setId(10L);
        documento.setAgendamento(agendamento);
        documento.setUrlDocumento("documentos/20/arquivo.png");
        return documento;
    }

    private ProcessamentoOcrMensagem novaMensagemReivindicada() {
        return new ProcessamentoOcrMensagem(
                10L,
                20L,
                "documentos/20/arquivo.png",
                LocalDateTime.of(2026, 7, 17, 10, 0),
                1);
    }
}
