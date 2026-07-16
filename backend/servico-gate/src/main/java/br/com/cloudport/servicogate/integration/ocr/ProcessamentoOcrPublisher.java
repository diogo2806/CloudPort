package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessamentoOcrPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrPublisher.class);

    private final ProcessamentoOcrExecutor executor;

    public ProcessamentoOcrPublisher(ProcessamentoOcrExecutor executor) {
        this.executor = executor;
    }

    public void enfileirarProcessamento(DocumentoAgendamento documento) {
        LOGGER.debug("event=ocr.processamento-local documentoId={} agendamentoId={}",
                documento.getId(),
                documento.getAgendamento() != null ? documento.getAgendamento().getId() : null);
        executor.processar(ProcessamentoOcrMensagem.from(documento));
    }
}
