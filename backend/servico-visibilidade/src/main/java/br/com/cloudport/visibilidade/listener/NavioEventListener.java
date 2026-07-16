package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.ProcessamentoEventoIdempotenteService;
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
    private static final String ORIGEM = "NAVIO";

    private final StatusNavioService statusNavioService;
    private final AlertasService alertasService;
    private final ProcessamentoEventoIdempotenteService processamentoEventoService;

    public NavioEventListener(StatusNavioService statusNavioService,
                              AlertasService alertasService,
                              ProcessamentoEventoIdempotenteService processamentoEventoService) {
        this.statusNavioService = statusNavioService;
        this.alertasService = alertasService;
        this.processamentoEventoService = processamentoEventoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_NAVIO_QUEUE)
    public void handleNavioEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        String navioId = texto(event, "navioId");

        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("Evento de navio sem eventType.");
        }
        if (!StringUtils.hasText(navioId)) {
            throw new IllegalArgumentException(
                    "Evento de navio " + eventType + " sem navioId.");
        }

        switch (eventType) {
            case "navio.arrived":
                processarUmaVez(event, eventType, eventoId -> {
                    statusNavioService.atualizarStatusNavio(navioId, "ancorando", null);
                    alertasService.resolverAlertasAtivos(navioId, TIPO_ALERTA_ATRASO);
                    LOGGER.info("Chegada de navio registrada. navioId={}", navioId);
                });
                break;
            case "navio.berth_assigned":
                String berco = texto(event, "berco");
                if (!StringUtils.hasText(berco)) {
                    throw new IllegalArgumentException(
                            "Evento navio.berth_assigned sem berco. navioId=" + navioId);
                }
                processarUmaVez(event, eventType, eventoId -> {
                    statusNavioService.atualizarStatusNavio(navioId, null, berco);
                    LOGGER.info("Berco atribuido ao navio. navioId={} berco={}", navioId, berco);
                });
                break;
            case "navio.operations_started":
                processarUmaVez(event, eventType, eventoId -> {
                    statusNavioService.atualizarStatusNavio(navioId, "operando", null);
                    LOGGER.info("Inicio das operacoes registrado. navioId={}", navioId);
                });
                break;
            case "navio.operations_completed":
                processarUmaVez(event, eventType, eventoId -> {
                    statusNavioService.atualizarStatusNavio(navioId, "pronto_para_partir", null);
                    LOGGER.info("Conclusao das operacoes registrada. navioId={}", navioId);
                });
                break;
            default:
                LOGGER.debug("Evento de navio sem processador registrado. eventType={} navioId={}",
                        eventType, navioId);
        }
    }

    private void processarUmaVez(Map<String, Object> event,
                                 String eventType,
                                 java.util.function.Consumer<String> efeito) {
        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM, eventType, event, efeito);
        if (!processado) {
            LOGGER.debug("Redelivery de evento de navio ignorado. eventType={}", eventType);
        }
    }

    private String texto(Map<String, Object> event, String chave) {
        if (event == null) {
            return null;
        }
        Object valor = event.get(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }
}
