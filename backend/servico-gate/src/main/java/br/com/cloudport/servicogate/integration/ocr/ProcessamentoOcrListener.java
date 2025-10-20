package br.com.cloudport.servicogate.integration.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProcessamentoOcrListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrListener.class);

    private final ObjectMapper objectMapper;
    private final ProcessamentoOcrExecutor executor;

    public ProcessamentoOcrListener(ObjectMapper objectMapper, ProcessamentoOcrExecutor executor) {
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    @RabbitListener(queues = "${cloudport.gate.ocr.solicitacao-queue}")
    public void receberMensagem(String payload) {
        try {
            ProcessamentoOcrMensagem mensagem = objectMapper.readValue(payload, ProcessamentoOcrMensagem.class);
            LOGGER.debug("event=ocr.recebido documentoId={} agendamentoId={}",
                    mensagem.getDocumentoId(), mensagem.getAgendamentoId());
            executor.processar(mensagem);
        } catch (Exception ex) {
            LOGGER.error("Falha ao processar mensagem de OCR", ex);
            throw new AmqpRejectAndDontRequeueException("Falha ao processar mensagem de OCR", ex);
        }
    }
}
