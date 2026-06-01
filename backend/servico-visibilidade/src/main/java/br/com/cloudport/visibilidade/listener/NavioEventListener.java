package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NavioEventListener {

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_NAVIO_QUEUE)
    public void handleNavioEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        System.out.println("[NavioEventListener] Evento recebido: " + eventType);

        if ("navio.arrived".equals(eventType)) {
            // TODO: Criar/atualizar StatusNavio + resetar alertas
            System.out.println("Navio chegou");
        }

        if ("navio.berth_assigned".equals(eventType)) {
            // TODO: Atualizar bercoAlocado no StatusNavio
            System.out.println("Berço atribuído ao navio");
        }

        if ("navio.operations_started".equals(eventType)) {
            // TODO: Atualizar status operacional
            System.out.println("Operações do navio iniciadas");
        }

        if ("navio.operations_completed".equals(eventType)) {
            // TODO: Marcar navio pronto para partida
            System.out.println("Operações do navio concluídas");
        }
    }
}