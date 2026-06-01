package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NavioEventListener {

    @Autowired
    private StatusNavioService statusNavioService;

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_NAVIO_QUEUE)
    public void handleNavioEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String navioId = (String) event.get("navioId");
        String berco = (String) event.get("berco");

        System.out.println("[NavioEventListener] Evento: " + eventType + " | Navio: " + navioId);

        if ("navio.arrived".equals(eventType) && navioId != null) {
            System.out.println("--> Navio " + navioId + " chegou. Atualizando status e resetando alertas.");
            statusNavioService.atualizarStatusNavio(navioId, "ancorando", null);
            // TODO: Resetar alertas anteriores do navio
        }

        if ("navio.berth_assigned".equals(eventType) && navioId != null) {
            System.out.println("--> Berço " + berco + " atribuído ao navio " + navioId);
            statusNavioService.atualizarStatusNavio(navioId, null, berco);
        }

        if ("navio.operations_started".equals(eventType) && navioId != null) {
            System.out.println("--> Operações iniciadas para o navio " + navioId);
            statusNavioService.atualizarStatusNavio(navioId, "operando", null);
        }

        if ("navio.operations_completed".equals(eventType) && navioId != null) {
            System.out.println("--> Operações concluídas. Navio pronto para partida.");
            statusNavioService.atualizarStatusNavio(navioId, "pronto_para_partir", null);
        }
    }
}