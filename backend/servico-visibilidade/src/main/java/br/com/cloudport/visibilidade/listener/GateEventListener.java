package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.service.EventoProcessadoService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GateEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateEventListener.class);
    private static final String CONSUMIDOR = "GATE";

    private final MovimentoConteinerService movimentoConteinerService;
    private final EventoProcessadoService eventoProcessadoService;

    public GateEventListener(MovimentoConteinerService movimentoConteinerService,
                             EventoProcessadoService eventoProcessadoService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.eventoProcessadoService = eventoProcessadoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        boolean processado = eventoProcessadoService.processarUmaVez(
                CONSUMIDOR, event, this::processarEvento);
        if (!processado) {
            LOGGER.info("Redelivery de evento do gate ignorado. identidade={}",
                    EventoRecebido.de(event).getIdentidade());
        }
    }

    private void processarEvento(EventoRecebido event) {
        String eventType = event.getTipo();
        String containerId = event.primeiroTexto("containerId", "codigoConteiner");
        String responsavel = event.primeiroTexto("responsavel", "usuario", "operatorId");

        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento de gate {} ignorado porque containerId nao foi informado.", eventType);
            return;
        }

        switch (eventType) {
            case "gate.container.entered":
                movimentoConteinerService.registrarEntradaGate(
                        event.getIdentidade(), containerId, responsavel);
                LOGGER.info("Entrada de conteiner registrada. containerId={}", containerId);
                break;
            case "gate.container.exited":
                movimentoConteinerService.registrarSaidaGate(
                        event.getIdentidade(), containerId, responsavel);
                LOGGER.info("Saida de conteiner registrada. containerId={}", containerId);
                break;
            default:
                LOGGER.debug("Evento de gate sem processador registrado. eventType={} containerId={}",
                        eventType, containerId);
        }
    }
}
