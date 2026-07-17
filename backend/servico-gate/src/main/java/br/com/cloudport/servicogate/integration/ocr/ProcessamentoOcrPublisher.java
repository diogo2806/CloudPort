package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ProcessamentoOcrPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrPublisher.class);

    private final ProcessamentoOcrExecutor processamentoOcrExecutor;
    private final ProcessamentoOcrReivindicacaoService reivindicacaoService;
    private final Executor ocrTaskExecutor;

    public ProcessamentoOcrPublisher(
            ProcessamentoOcrExecutor processamentoOcrExecutor,
            ProcessamentoOcrReivindicacaoService reivindicacaoService,
            @Qualifier("ocrTaskExecutor") Executor ocrTaskExecutor) {
        this.processamentoOcrExecutor = processamentoOcrExecutor;
        this.reivindicacaoService = reivindicacaoService;
        this.ocrTaskExecutor = ocrTaskExecutor;
    }

    public void enfileirarProcessamento(DocumentoAgendamento documento) {
        documento.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);
        documento.setMensagemValidacao("Documento aguardando processamento OCR.");
        documento.setProximaTentativaOcrEm(null);
        documento.setProcessamentoOcrIniciadoEm(null);
        Long documentoId = documento.getId();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    reivindicarESubmeterComTratamento(documentoId);
                }
            });
            return;
        }
        reivindicarESubmeterComTratamento(documentoId);
    }

    public boolean reivindicarESubmeter(Long documentoId) {
        Optional<ProcessamentoOcrMensagem> mensagem = reivindicacaoService.reivindicar(documentoId);
        if (!mensagem.isPresent()) {
            return false;
        }
        return submeter(mensagem.get());
    }

    private void reivindicarESubmeterComTratamento(Long documentoId) {
        try {
            reivindicarESubmeter(documentoId);
        } catch (RuntimeException ex) {
            LOGGER.error("event=ocr.reivindicacao-falhou documentoId={}", documentoId, ex);
        }
    }

    private boolean submeter(ProcessamentoOcrMensagem mensagem) {
        try {
            ocrTaskExecutor.execute(() -> processarComTratamento(mensagem));
            LOGGER.debug("event=ocr.solicitacao-submetida documentoId={} agendamentoId={} tentativa={}",
                    mensagem.getDocumentoId(), mensagem.getAgendamentoId(), mensagem.getTentativa());
            return true;
        } catch (RejectedExecutionException ex) {
            liberarReivindicacao(mensagem, "Executor OCR indisponível antes do início do processamento.");
            LOGGER.warn("event=ocr.executor-indisponivel documentoId={} motivo={}",
                    mensagem.getDocumentoId(), ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            liberarReivindicacao(mensagem, "Falha ao submeter o documento ao executor OCR.");
            LOGGER.error("event=ocr.submissao-falhou documentoId={}", mensagem.getDocumentoId(), ex);
            return false;
        }
    }

    private void processarComTratamento(ProcessamentoOcrMensagem mensagem) {
        try {
            processamentoOcrExecutor.processar(mensagem);
        } catch (RuntimeException ex) {
            liberarReivindicacao(mensagem, "Falha inesperada antes da conclusão do processamento OCR.");
            LOGGER.error("event=ocr.processamento-nao-iniciado documentoId={} tentativa={}",
                    mensagem.getDocumentoId(), mensagem.getTentativa(), ex);
        }
    }

    private void liberarReivindicacao(ProcessamentoOcrMensagem mensagem, String motivo) {
        try {
            reivindicacaoService.liberarReivindicacao(mensagem, motivo);
        } catch (RuntimeException ex) {
            LOGGER.error("event=ocr.liberacao-reivindicacao-falhou documentoId={}",
                    mensagem.getDocumentoId(), ex);
        }
    }
}
