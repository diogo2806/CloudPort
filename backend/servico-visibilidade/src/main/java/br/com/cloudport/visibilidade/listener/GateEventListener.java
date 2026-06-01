package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.RastreamentoConteinerService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GateEventListener {

    @Autowired
    private RastreamentoConteinerService rastreamentoService;

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String containerId = (String) event.get("containerId");

        System.out.println("[GateEventListener] Evento: " + eventType + " | Container: " + containerId);

        if ("gate.container.entered".equals(eventType) && containerId != null) {
            System.out.println("--> Container " + containerId + " entrou no portão. Atualizando status para 'no_yard'");
            // TODO: Chamar service para atualizar ConteinerLocalizacao
        }

        if ("gate.container.exited".equals(eventType) && containerId != null) {
            System.out.println("--> Container " + containerId + " saiu do portão.");
        }

        if ("gate.processing_time".equals(eventType)) {
            System.out.println("--> Métrica de tempo de processamento do Gate recebida.");
        }
    }
}