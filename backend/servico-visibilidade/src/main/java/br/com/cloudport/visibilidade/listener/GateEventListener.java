package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import br.com.cloudport.visibilidade.service.ProcessamentoEventoIdempotenteService;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GateEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateEventListener.class);

    private final MovimentoConteinerService movimentoConteinerService;
    private final ProcessamentoEventoIdempotenteService processamentoEventoIdempotenteService;

    public GateEventListener(MovimentoConteinerService movimentoConteinerService,
                             ProcessamentoEventoIdempotenteService processamentoEventoIdempotenteService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.processamentoEventoIdempotenteService = processamentoEventoIdempotenteService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        String containerId = primeiroTexto(event, "containerId", "codigoConteiner");
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
                processarUmaVez(event, identidadeEvento -> {
                    movimentoConteinerService.registrarEntradaGate(
                            identidadeEvento, containerId, responsavel);
                    LOGGER.info("Entrada de conteiner registrada. containerId={}", containerId);
                });
                break;
            case "gate.container.exited":
                processarUmaVez(event, identidadeEvento -> {
                    movimentoConteinerService.registrarSaidaGate(
                            identidadeEvento, containerId, responsavel);
                    LOGGER.info("Saida de conteiner registrada. containerId={}", containerId);
                });
                break;
            default:
                LOGGER.debug("Evento de gate sem processador registrado. eventType={} containerId={}",
                        eventType, containerId);
        }
    }

    private void processarUmaVez(Map<String, Object> event, Consumer<String> processamento) {
        boolean processado = processamentoEventoIdempotenteService.processarUmaVez(event, processamento);
        if (!processado) {
            LOGGER.info("Redelivery de evento de gate ignorada. eventType={} identidade={}",
                    texto(event, "eventType"), primeiroTexto(event, "eventId", "messageId"));
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
