package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.config.OcrIntegrationProperties;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProcessamentoOcrPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OcrIntegrationProperties properties;

    public ProcessamentoOcrPublisher(RabbitTemplate rabbitTemplate,
                                     ObjectMapper objectMapper,
                                     OcrIntegrationProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void enfileirarProcessamento(DocumentoAgendamento documento) {
        ProcessamentoOcrMensagem mensagem = ProcessamentoOcrMensagem.from(documento);
        try {
            String payload = objectMapper.writeValueAsString(mensagem);
            rabbitTemplate.convertAndSend(properties.getSolicitacaoQueue(), payload);
            LOGGER.debug("event=ocr.enfileirar documentoId={} agendamentoId={}",
                    mensagem.getDocumentoId(), mensagem.getAgendamentoId());
        } catch (JsonProcessingException ex) {
            LOGGER.error("Falha ao serializar solicitação de OCR", ex);
            throw new BusinessException("Falha ao solicitar validação automática do documento.", ex);
        }
    }
}
