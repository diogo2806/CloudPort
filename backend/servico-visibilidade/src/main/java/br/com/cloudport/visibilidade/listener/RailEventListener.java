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
        String containerId = (String) event.get("containerId");
        String origem = (String) event.get("origem");
        String destino = (String) event.get("destino");

        System.out.println("[RailEventListener] Evento: " + eventType);

        if ("rail.container.moved".equals(eventType) && containerId != null) {
            System.out.println("--> Container " + containerId + " movimentado via rail de " + origem + " para " + destino);
            // TODO: Atualizar rastreamento/histórico do container via integração rail
        }
    }
}