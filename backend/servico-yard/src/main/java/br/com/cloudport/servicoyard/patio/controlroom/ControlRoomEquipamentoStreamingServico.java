package br.com.cloudport.servicoyard.patio.controlroom;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ControlRoomEquipamentoStreamingServico {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final CopyOnWriteArrayList<SseEmitter> assinantes = new CopyOnWriteArrayList<>();

    public SseEmitter assinar(ControlRoomEquipamentoDtos.Resumo resumo) {
        SseEmitter emissor = new SseEmitter(TIMEOUT_MILLIS);
        assinantes.add(emissor);
        emissor.onCompletion(() -> assinantes.remove(emissor));
        emissor.onTimeout(() -> assinantes.remove(emissor));
        emissor.onError(erro -> assinantes.remove(emissor));
        enviar(emissor, "SNAPSHOT", Map.of("resumo", resumo));
        return emissor;
    }

    public void publicar(String tipo, Object dados) {
        assinantes.forEach(emissor -> enviar(emissor, tipo, dados));
    }

    private void enviar(SseEmitter emissor, String tipo, Object dados) {
        String eventoId = UUID.randomUUID().toString();
        try {
            emissor.send(SseEmitter.event()
                    .id(eventoId)
                    .name("cloudport.control-room.v1")
                    .reconnectTime(2000L)
                    .data(new EventoControlRoom("1.0", tipo, eventoId, Instant.now().toString(), dados)));
        } catch (IOException | IllegalStateException erro) {
            assinantes.remove(emissor);
            emissor.completeWithError(erro);
        }
    }

    public record EventoControlRoom(
            String schemaVersion,
            String tipo,
            String eventId,
            String ocorridoEm,
            Object dados
    ) {
    }
}
