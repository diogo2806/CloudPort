package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.contracts.evento.EventoIntegracaoV1;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EventoIntegracaoPublicador {

    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, List<SseEmitter>> emissoresPorVisita = new ConcurrentHashMap<>();
    private final List<SseEmitter> emissoresGlobais = new CopyOnWriteArrayList<>();
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final ApplicationEventPublisher applicationEventPublisher;

    public EventoIntegracaoPublicador(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider,
                                       ApplicationEventPublisher applicationEventPublisher) {
        this.messagingTemplateProvider = messagingTemplateProvider;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public SseEmitter assinar(Long visitaId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        List<SseEmitter> lista = visitaId == null
                ? emissoresGlobais
                : emissoresPorVisita.computeIfAbsent(visitaId, chave -> new CopyOnWriteArrayList<>());
        lista.add(emitter);
        Runnable remover = () -> remover(visitaId, emitter);
        emitter.onCompletion(remover);
        emitter.onTimeout(remover);
        emitter.onError(erro -> remover.run());
        try {
            EventoIntegracaoV1<Map<String, Object>> conectado = EventoIntegracaoV1.criar(
                    "integration.stream.connected",
                    null,
                    "cloudport-navio",
                    Map.of("visitaId", visitaId == null ? "todas" : visitaId)
            );
            emitter.send(SseEmitter.event()
                    .id(conectado.eventId().toString())
                    .name(conectado.eventType())
                    .data(conectado));
        } catch (IOException ex) {
            remover.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    public void publicar(Long visitaId, EventoVisitaNavioDTO evento, String correlationId) {
        EventoIntegracaoV1<EventoVisitaNavioDTO> envelope = EventoIntegracaoV1.criar(
                "vessel-visit.event.recorded",
                correlationId,
                "servico-navio-siderurgico",
                evento
        );
        enviar(emissoresGlobais, envelope);
        enviar(emissoresPorVisita.getOrDefault(visitaId, List.of()), envelope);
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/v1/vessel-visits/" + visitaId + "/events", envelope);
            messagingTemplate.convertAndSend("/topic/v1/integrations/events", envelope);
        }
        applicationEventPublisher.publishEvent(envelope);
    }

    private void enviar(List<SseEmitter> emissores, EventoIntegracaoV1<?> envelope) {
        emissores.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(envelope.eventId().toString())
                        .name(envelope.eventType())
                        .data(envelope));
            } catch (IOException ex) {
                emissores.remove(emitter);
                emitter.completeWithError(ex);
            }
        });
    }

    private void remover(Long visitaId, SseEmitter emitter) {
        if (visitaId == null) {
            emissoresGlobais.remove(emitter);
            return;
        }
        List<SseEmitter> emissores = emissoresPorVisita.get(visitaId);
        if (emissores == null) {
            return;
        }
        emissores.remove(emitter);
        if (emissores.isEmpty()) {
            emissoresPorVisita.remove(visitaId);
        }
    }
}
