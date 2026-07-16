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
public class RailEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RailEventListener.class);

    private final MovimentoConteinerService movimentoConteinerService;

    public RailEventListener(MovimentoConteinerService movimentoConteinerService) {
        this.movimentoConteinerService = movimentoConteinerService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_RAIL_QUEUE)
    public void handleRailEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        if (!"rail.container.moved".equals(eventType)) {
            if (!StringUtils.hasText(eventType)) {
                LOGGER.warn("Evento ferroviario ignorado porque eventType nao foi informado.");
            } else {
                LOGGER.debug("Evento ferroviario sem processador registrado. eventType={}", eventType);
            }
            return;
        }

        String containerId = texto(event, "containerId");
        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento rail.container.moved ignorado porque containerId nao foi informado.");
            return;
        }

        String origem = texto(event, "origem");
        String destino = texto(event, "destino");
        String equipamento = primeiroTexto(event, "equipamentoId", "equipamento", "locomotivaId");
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");

        movimentoConteinerService.registrarMovimentoRail(
                containerId, origem, destino, equipamento, responsavel);
        LOGGER.info("Movimento ferroviario registrado. containerId={} origem={} destino={}",
                containerId, origem, destino);
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
