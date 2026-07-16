package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import br.com.cloudport.visibilidade.service.ProcessamentoEventoIdempotenteService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GateEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateEventListener.class);
    private static final String ORIGEM = "GATE";

    private final MovimentoConteinerService movimentoConteinerService;
    private final ProcessamentoEventoIdempotenteService processamentoEventoService;

    public GateEventListener(MovimentoConteinerService movimentoConteinerService,
                             ProcessamentoEventoIdempotenteService processamentoEventoService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.processamentoEventoService = processamentoEventoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("Evento de gate sem eventType.");
        }

        switch (eventType) {
            case "gate.container.entered":
                processarEntrada(event, eventType);
                break;
            case "gate.container.exited":
                processarSaida(event, eventType);
                break;
            default:
                LOGGER.debug("Evento de gate sem processador registrado. eventType={}", eventType);
        }
    }

    private void processarEntrada(Map<String, Object> event, String eventType) {
        String containerId = containerIdObrigatorio(event, eventType);
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");
        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> movimentoConteinerService.registrarEntradaGate(
                        eventoId, containerId, responsavel));
        if (processado) {
            LOGGER.info("Entrada de conteiner registrada. containerId={}", containerId);
        } else {
            LOGGER.debug("Redelivery de entrada de gate ignorado. containerId={}", containerId);
        }
    }

    private void processarSaida(Map<String, Object> event, String eventType) {
        String containerId = containerIdObrigatorio(event, eventType);
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");
        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> movimentoConteinerService.registrarSaidaGate(
                        eventoId, containerId, responsavel));
        if (processado) {
            LOGGER.info("Saida de conteiner registrada. containerId={}", containerId);
        } else {
            LOGGER.debug("Redelivery de saida de gate ignorado. containerId={}", containerId);
        }
    }

    private String containerIdObrigatorio(Map<String, Object> event, String eventType) {
        String containerId = primeiroTexto(event, "containerId", "codigoConteiner");
        if (!StringUtils.hasText(containerId)) {
            throw new IllegalArgumentException(
                    "Evento de gate " + eventType + " sem containerId.");
        }
        return containerId;
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
