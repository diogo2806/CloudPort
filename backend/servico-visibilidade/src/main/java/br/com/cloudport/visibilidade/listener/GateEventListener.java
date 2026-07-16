package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
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

    private final MovimentoConteinerService movimentoConteinerService;

    public GateEventListener(MovimentoConteinerService movimentoConteinerService) {
        this.movimentoConteinerService = movimentoConteinerService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        String containerId = texto(event, "containerId");
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");

        if (!StringUtils.hasText(eventType)) {
            LOGGER.warn("Evento de gate ignorado porque eventType nao foi informado.");
            return;
        }

        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento de gate {} ignorado porque containerId nao foi informado.", eventType);
            return;
        }

        switch (eventType) {
            case "gate.container.entered":
                movimentoConteinerService.registrarEntradaGate(containerId, responsavel);
                LOGGER.info("Entrada de conteiner registrada. containerId={}", containerId);
                break;
            case "gate.container.exited":
                movimentoConteinerService.registrarSaidaGate(containerId, responsavel);
                LOGGER.info("Saida de conteiner registrada. containerId={}", containerId);
                break;
            default:
                LOGGER.debug("Evento de gate sem processador registrado. eventType={} containerId={}",
                        eventType, containerId);
        }
    }

    private String primeiroTexto(Map<String, Object> event, String... chaves) {
        for (String chave : chaves) {
            String valor = texto(event, chave);
            if (StringUtils.hasText(valor)) {
                return valor;
            }
        }
        return null;
    }

    private String texto(Map<String, Object> event, String chave) {
        if (event == null) {
            return null;
        }
        Object valor = event.get(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }
}
