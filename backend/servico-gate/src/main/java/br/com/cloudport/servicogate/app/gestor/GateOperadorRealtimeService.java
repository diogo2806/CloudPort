package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.GateOperadorEventoDTO;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class GateOperadorRealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateOperadorRealtimeService.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final List<SseEmitter> emissores = new CopyOnWriteArrayList<>();

    public SseEmitter registrar() {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        emissores.add(emitter);
        emitter.onCompletion(() -> remover(emitter));
        emitter.onTimeout(() -> remover(emitter));
        emitter.onError(throwable -> remover(emitter));
        return emitter;
    }

    public void publicarEvento(GateOperadorEventoDTO evento) {
        if (evento == null || emissores.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emissores) {
            try {
                emitter.send(evento);
            } catch (IOException ex) {
                LOGGER.debug("Falha ao enviar evento SSE do gate", ex);
                emitter.completeWithError(ex);
                emissores.remove(emitter);
            }
        }
    }

    private void remover(SseEmitter emitter) {
        emissores.remove(emitter);
    }
}
