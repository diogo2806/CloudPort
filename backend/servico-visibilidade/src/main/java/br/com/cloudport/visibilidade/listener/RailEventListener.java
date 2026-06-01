package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RailEventListener {

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_RAIL_QUEUE)
    public void handleRailEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        System.out.println("[RailEventListener] Evento recebido: " + eventType);

        if ("rail.container.moved".equals(eventType)) {
            // TODO: Rastrear movimentação via rail (se integrado)
            System.out.println("Container movimentado via rail");
        }
    }
}