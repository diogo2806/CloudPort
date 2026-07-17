package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cloudport.runtime",
        name = "jobs-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ProcessamentoOcrRecuperacaoJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrRecuperacaoJob.class);

    private final DocumentoAgendamentoRepository documentoAgendamentoRepository;
    private final ProcessamentoOcrPublisher processamentoOcrPublisher;
    private final Duration processamentoExpirado;
    private final int maxTentativas;

    public ProcessamentoOcrRecuperacaoJob(
            DocumentoAgendamentoRepository documentoAgendamentoRepository,
            ProcessamentoOcrPublisher processamentoOcrPublisher,
            @Value("${cloudport.gate.ocr.processamento-expirado:PT10M}") Duration processamentoExpirado,
            @Value("${cloudport.gate.ocr.max-tentativas:3}") int maxTentativas) {
        this.documentoAgendamentoRepository = documentoAgendamentoRepository;
        this.processamentoOcrPublisher = processamentoOcrPublisher;
        this.processamentoExpirado = processamentoExpirado;
        this.maxTentativas = maxTentativas;
    }

    @Scheduled(fixedDelayString = "${cloudport.gate.ocr.recuperacao-ms:60000}")
    public void recuperarProcessamentosPendentes() {
        LocalDateTime agora = LocalDateTime.now();
        Set<Long> documentos = new LinkedHashSet<>();

        documentoAgendamentoRepository
                .findTop100ByStatusValidacaoOrderByUpdatedAtAsc(StatusValidacaoDocumento.PENDENTE)
                .forEach(documento -> documentos.add(documento.getId()));
        documentoAgendamentoRepository
                .findTop100ByStatusValidacaoAndProcessamentoOcrIniciadoEmBeforeOrderByProcessamentoOcrIniciadoEmAsc(
                        StatusValidacaoDocumento.PROCESSANDO,
                        agora.minus(processamentoExpirado))
                .forEach(documento -> documentos.add(documento.getId()));
        documentoAgendamentoRepository
                .findTop100ByStatusValidacaoAndProximaTentativaOcrEmLessThanEqualAndTentativasOcrLessThanOrderByProximaTentativaOcrEmAsc(
                        StatusValidacaoDocumento.FALHA,
                        agora,
                        maxTentativas)
                .forEach(documento -> documentos.add(documento.getId()));

        documentos.forEach(this::reivindicarESubmeter);
    }

    private void reivindicarESubmeter(Long documentoId) {
        try {
            processamentoOcrPublisher.reivindicarESubmeter(documentoId);
        } catch (RuntimeException ex) {
            LOGGER.error("event=ocr.recuperacao-falhou documentoId={}", documentoId, ex);
        }
    }
}
