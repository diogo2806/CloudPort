package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.EventoMovimentoPatioDto;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventoMovimentoPatio {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorEventoMovimentoPatio.class);
    private static final String TIPO_EVENTO = "yard.movimento.registrado";
    private static final int VERSAO_EVENTO = 1;

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final boolean rabbitEnabled;

    public PublicadorEventoMovimentoPatio(RabbitTemplate rabbitTemplate,
                                          @Value("${cloudport.yard.eventos.exchange}") String exchange,
                                          @Value("${cloudport.yard.eventos.routing-movimento}") String routingKey,
                                          @Value("${cloudport.messaging.rabbit.enabled:false}") boolean rabbitEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.rabbitEnabled = rabbitEnabled;
    }

    public void publicar(EventoMovimentoPatioDto evento) {
        if (!rabbitEnabled) {
            LOGGER.debug("Evento de movimento do pátio não publicado porque o RabbitMQ está desabilitado.");
            return;
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", TIPO_EVENTO);
        envelope.put("eventVersion", VERSAO_EVENTO);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("source", "servico-yard");
        envelope.put("data", evento);
        rabbitTemplate.convertAndSend(exchange, routingKey, envelope);
    }
}
