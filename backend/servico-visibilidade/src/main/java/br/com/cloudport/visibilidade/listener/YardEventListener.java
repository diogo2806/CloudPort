package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class YardEventListener {

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_YARD_QUEUE)
    public void handleYardEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        System.out.println("[YardEventListener] Evento recebido: " + eventType);

        if ("yard.container.stored".equals(eventType)) {
            // TODO: Atualizar ConteinerLocalizacao com zona e posição
            System.out.println("Container armazenado no yard");
        }

        if ("yard.container.retrieved".equals(eventType)) {
            // TODO: Atualizar status para aguardando_saida
            System.out.println("Container retirado do yard");
        }

        if ("yard.capacity_updated".equals(eventType)) {
            // TODO: Atualizar CapacidadeYard
            System.out.println("Capacidade do yard atualizada");
        }
    }
}