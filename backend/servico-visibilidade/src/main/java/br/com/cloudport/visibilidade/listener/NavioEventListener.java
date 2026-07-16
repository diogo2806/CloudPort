package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.EventoProcessadoService;
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
    private static final String CONSUMIDOR = "NAVIO";

    private final StatusNavioService statusNavioService;
    private final AlertasService alertasService;
    private final EventoProcessadoService eventoProcessadoService;

    public NavioEventListener(StatusNavioService statusNavioService,
                              AlertasService alertasService,
                              EventoProcessadoService eventoProcessadoService) {
        this.statusNavioService = statusNavioService;
        this.alertasService = alertasService;
        this.eventoProcessadoService = eventoProcessadoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_NAVIO_QUEUE)
    public void handleNavioEvent(Map<String, Object> event) {
        boolean processado = eventoProcessadoService.processarUmaVez(
                CONSUMIDOR, event, this::processarEvento);
        if (!processado) {
            LOGGER.info("Redelivery de evento de navio ignorado. identidade={}",
                    EventoRecebido.de(event).getIdentidade());
        }
    }

    private void processarEvento(EventoRecebido event) {
        String eventType = event.getTipo();
        String navioId = event.texto("navioId");

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

    private void processarBercoAtribuido(EventoRecebido event, String navioId) {
        String berco = event.texto("berco");
        if (!StringUtils.hasText(berco)) {
            LOGGER.warn("Evento navio.berth_assigned ignorado porque berco nao foi informado. navioId={}",
                    navioId);
            return;
        }
        statusNavioService.atualizarStatusNavio(navioId, null, berco);
        LOGGER.info("Berco atribuido ao navio. navioId={} berco={}", navioId, berco);
    }
}
