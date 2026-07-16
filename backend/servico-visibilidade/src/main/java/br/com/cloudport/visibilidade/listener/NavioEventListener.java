package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NavioEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavioEventListener.class);
    private static final String TIPO_ALERTA_ATRASO = "ATRASO_NAVIO";

    private final StatusNavioService statusNavioService;
    private final AlertasService alertasService;

    public NavioEventListener(StatusNavioService statusNavioService,
                              AlertasService alertasService) {
        this.statusNavioService = statusNavioService;
        this.alertasService = alertasService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_NAVIO_QUEUE)
    public void handleNavioEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        String navioId = texto(event, "navioId");

        if (!StringUtils.hasText(eventType)) {
            LOGGER.warn("Evento de navio ignorado porque eventType nao foi informado.");
            return;
        }
        if (!StringUtils.hasText(navioId)) {
            LOGGER.warn("Evento de navio {} ignorado porque navioId nao foi informado.", eventType);
            return;
        }

        switch (eventType) {
            case "navio.arrived":
                statusNavioService.atualizarStatusNavio(navioId, "ancorando", null);
                alertasService.resolverAlertasAtivos(navioId, TIPO_ALERTA_ATRASO);
                LOGGER.info("Chegada de navio registrada. navioId={}", navioId);
                break;
            case "navio.berth_assigned":
                processarBercoAtribuido(event, navioId);
                break;
            case "navio.operations_started":
                statusNavioService.atualizarStatusNavio(navioId, "operando", null);
                LOGGER.info("Inicio das operacoes registrado. navioId={}", navioId);
                break;
            case "navio.operations_completed":
                statusNavioService.atualizarStatusNavio(navioId, "pronto_para_partir", null);
                LOGGER.info("Conclusao das operacoes registrada. navioId={}", navioId);
                break;
            default:
                LOGGER.debug("Evento de navio sem processador registrado. eventType={} navioId={}",
                        eventType, navioId);
        }
    }

    private void processarBercoAtribuido(Map<String, Object> event, String navioId) {
        String berco = texto(event, "berco");
        if (!StringUtils.hasText(berco)) {
            LOGGER.warn("Evento navio.berth_assigned ignorado porque berco nao foi informado. navioId={}",
                    navioId);
            return;
        }
        statusNavioService.atualizarStatusNavio(navioId, null, berco);
        LOGGER.info("Berco atribuido ao navio. navioId={} berco={}", navioId, berco);
    }

    private String texto(Map<String, Object> event, String chave) {
        if (event == null) {
            return null;
        }
        Object valor = event.get(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }
}
