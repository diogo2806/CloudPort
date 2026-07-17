package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ProcessamentoOcrPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrPublisher.class);

    private final ProcessamentoOcrExecutor processamentoOcrExecutor;
    private final DocumentoAgendamentoRepository documentoAgendamentoRepository;
    private final Executor ocrTaskExecutor;
    private final Duration processamentoExpirado;
    private final int maxTentativas;

    public ProcessamentoOcrPublisher(ProcessamentoOcrExecutor processamentoOcrExecutor,
                                    DocumentoAgendamentoRepository documentoAgendamentoRepository,
                                    @Qualifier("ocrTaskExecutor") Executor ocrTaskExecutor,
                                    @Value("${cloudport.gate.ocr.processamento-expirado:PT10M}") Duration processamentoExpirado,
                                    @Value("${cloudport.gate.ocr.max-tentativas:3}") int maxTentativas) {
        this.processamentoOcrExecutor = processamentoOcrExecutor;
        this.documentoAgendamentoRepository = documentoAgendamentoRepository;
        this.ocrTaskExecutor = ocrTaskExecutor;
        this.processamentoExpirado = processamentoExpirado;
        this.maxTentativas = maxTentativas;
    }

    public void enfileirarProcessamento(DocumentoAgendamento documento) {
        documento.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);
        documento.setMensagemValidacao("Documento aguardando processamento OCR.");
        documento.setProximaTentativaOcrEm(null);
        documento.setProcessamentoOcrIniciadoEm(null);
        ProcessamentoOcrMensagem mensagem = ProcessamentoOcrMensagem.from(documento);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submeter(mensagem);
                }
            });
            return;
        }
        submeter(mensagem);
    }

    @Scheduled(fixedDelayString = "${cloudport.gate.ocr.recuperacao-ms:60000}")
    public void recuperarProcessamentosPendentes() {
        Map<Long, ProcessamentoOcrMensagem> mensagens = new LinkedHashMap<>();
        documentoAgendamentoRepository
                .findTop100ByStatusValidacaoOrderByUpdatedAtAsc(StatusValidacaoDocumento.PENDENTE)
                .forEach(documento -> mensagens.put(documento.getId(), ProcessamentoOcrMensagem.from(documento)));
        documentoAgendamentoRepository
                .findTop100ByStatusValidacaoAndProcessamentoOcrIniciadoEmBeforeOrderByProcessamentoOcrIniciadoEmAsc(
                        StatusValidacaoDocumento.PROCESSANDO,
                        LocalDateTime.now().minus(processamentoExpirado))
                .forEach(documento -> mensagens.put(documento.getId(), ProcessamentoOcrMensagem.from(documento)));
        documentoAgendamentoRepository
                .findTop100ByStatusValidacaoAndProximaTentativaOcrEmLessThanEqualAndTentativasOcrLessThanOrderByProximaTentativaOcrEmAsc(
                        StatusValidacaoDocumento.FALHA,
                        LocalDateTime.now(),
                        maxTentativas)
                .forEach(documento -> mensagens.put(documento.getId(), ProcessamentoOcrMensagem.from(documento)));
        mensagens.values().forEach(this::submeter);
    }

    private void submeter(ProcessamentoOcrMensagem mensagem) {
        try {
            ocrTaskExecutor.execute(() -> processamentoOcrExecutor.processar(mensagem));
            LOGGER.debug("event=ocr.solicitacao-submetida documentoId={} agendamentoId={}",
                    mensagem.getDocumentoId(), mensagem.getAgendamentoId());
        } catch (RejectedExecutionException ex) {
            LOGGER.warn("event=ocr.executor-indisponivel documentoId={} motivo={}",
                    mensagem.getDocumentoId(), ex.getMessage());
        }
    }
}
