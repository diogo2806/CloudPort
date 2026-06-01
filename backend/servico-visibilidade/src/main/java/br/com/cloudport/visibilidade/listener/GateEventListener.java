package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GateEventListener {

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        System.out.println("[GateEventListener] Evento recebido: " + eventType);

        if ("gate.container.entered".equals(eventType)) {
            // TODO: Atualizar ConteinerLocalizacao.status = "no_yard"
            System.out.println("Container entrou no gate -> atualizar localizacao para no_yard");
        }

        if ("gate.container.exited".equals(eventType)) {
            // TODO: Atualizar status para saiu_do_porto
            System.out.println("Container saiu do porto");
        }

        if ("gate.processing_time".equals(eventType)) {
            // TODO: Calcular métrica de gargalo de gate
            System.out.println("Métrica de tempo de processamento do gate");
        }
    }
}