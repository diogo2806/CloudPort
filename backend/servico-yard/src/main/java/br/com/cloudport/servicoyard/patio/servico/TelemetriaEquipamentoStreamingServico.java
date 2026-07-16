package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.TelemetriaEquipamentoPatioDto;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TelemetriaEquipamentoStreamingServico {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final CopyOnWriteArrayList<SseEmitter> assinantes = new CopyOnWriteArrayList<>();

    public SseEmitter assinar(String ultimoEventoId, List<TelemetriaEquipamentoPatioDto> snapshot) {
        SseEmitter emissor = new SseEmitter(TIMEOUT_MILLIS);
        assinantes.add(emissor);
        emissor.onCompletion(() -> assinantes.remove(emissor));
        emissor.onTimeout(() -> assinantes.remove(emissor));
        emissor.onError(erro -> assinantes.remove(emissor));
        try {
            emissor.send(SseEmitter.event()
                    .id("telemetria-snapshot-" + System.currentTimeMillis())
                    .name("cloudport.telemetria.v1")
                    .reconnectTime(2000L)
                    .data(new EnvelopeTelemetria("1.0", "SNAPSHOT", ultimoEventoId, snapshot)));
        } catch (IOException erro) {
            assinantes.remove(emissor);
            emissor.completeWithError(erro);
        }
        return emissor;
    }

    public void publicar(TelemetriaEquipamentoPatioDto telemetria) {
        String id = telemetria.equipamento() + "-" + telemetria.sequencia();
        EnvelopeTelemetria envelope = new EnvelopeTelemetria("1.0", "ATUALIZACAO", id, List.of(telemetria));
        assinantes.forEach(emissor -> enviar(emissor, id, envelope));
    }

    private void enviar(SseEmitter emissor, String id, EnvelopeTelemetria envelope) {
        try {
            emissor.send(SseEmitter.event()
                    .id(id)
                    .name("cloudport.telemetria.v1")
                    .reconnectTime(2000L)
                    .data(envelope));
        } catch (IOException | IllegalStateException erro) {
            assinantes.remove(emissor);
            emissor.completeWithError(erro);
        }
    }

    public record EnvelopeTelemetria(
            String schemaVersion,
            String tipo,
            String eventId,
            List<TelemetriaEquipamentoPatioDto> equipamentos
    ) {
    }
}
