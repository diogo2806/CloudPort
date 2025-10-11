package br.com.cloudport.servicogate.integration.hardware;

import br.com.cloudport.servicogate.config.HardwareIntegrationProperties;
import br.com.cloudport.servicogate.dto.GateDecisionDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class GateHardwarePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateHardwarePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final HardwareIntegrationProperties properties;
    private final ObjectMapper objectMapper;

    public GateHardwarePublisher(RabbitTemplate rabbitTemplate,
                                 HardwareIntegrationProperties properties,
                                 ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void publicarDecisaoEntrada(GateDecisionDTO decision, HardwareEventMessage event) {
        publicar(decision, event, properties.getDecisaoRoutingEntrada());
    }

    public void publicarDecisaoSaida(GateDecisionDTO decision, HardwareEventMessage event) {
        publicar(decision, event, properties.getDecisaoRoutingSaida());
    }

    private void publicar(GateDecisionDTO decision, HardwareEventMessage event, String routingKey) {
        HardwareDecisionMessage message = HardwareDecisionMessage.from(decision, event);
        try {
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(properties.getDecisaoExchange(), routingKey, payload);
        } catch (JsonProcessingException ex) {
            LOGGER.error("Falha ao serializar decisão de gate para publicação", ex);
        }
    }
}
