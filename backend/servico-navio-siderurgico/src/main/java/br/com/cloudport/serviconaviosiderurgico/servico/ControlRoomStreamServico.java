package br.com.cloudport.serviconaviosiderurgico.servico;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ControlRoomStreamServico implements InitializingBean, DisposableBean {

    private static final long RECONNECT_TIME_MILLIS = 3_000L;
    private static final long INTERVALO_SNAPSHOT_SEGUNDOS = 10L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> assinantes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService agendador;

    public ControlRoomStreamServico() {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "cloudport-control-room-sse");
            thread.setDaemon(true);
            return thread;
        };
        this.agendador = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void afterPropertiesSet() {
        agendador.scheduleWithFixedDelay(
                this::publicarSnapshotsPeriodicos,
                INTERVALO_SNAPSHOT_SEGUNDOS,
                INTERVALO_SNAPSHOT_SEGUNDOS,
                TimeUnit.SECONDS
        );
    }

    public SseEmitter assinar(Long visitaNavioId) {
        SseEmitter emitter = new SseEmitter(0L);
        assinantes.computeIfAbsent(visitaNavioId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remover(visitaNavioId, emitter));
        emitter.onTimeout(() -> remover(visitaNavioId, emitter));
        emitter.onError(error -> remover(visitaNavioId, emitter));
        enviar(visitaNavioId, emitter, "CONEXAO_INICIAL");
        return emitter;
    }

    public void publicarAtualizacao(Long visitaNavioId, String motivo) {
        CopyOnWriteArrayList<SseEmitter> emitters = assinantes.get(visitaNavioId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        emitters.forEach(emitter -> enviar(visitaNavioId, emitter, motivo));
    }

    private void publicarSnapshotsPeriodicos() {
        assinantes.keySet().forEach(visitaNavioId -> publicarAtualizacao(visitaNavioId, "SNAPSHOT_PERIODICO"));
    }

    private void enviar(Long visitaNavioId, SseEmitter emitter, String motivo) {
        try {
            emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name("control-room")
                    .reconnectTime(RECONNECT_TIME_MILLIS)
                    .data(Map.of(
                            "tipo", "SNAPSHOT_INVALIDADO",
                            "motivo", motivo,
                            "visitaNavioId", visitaNavioId,
                            "timestamp", Instant.now().toString()
                    )));
        } catch (IOException | IllegalStateException ex) {
            remover(visitaNavioId, emitter);
        }
    }

    private void remover(Long visitaNavioId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = assinantes.get(visitaNavioId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            assinantes.remove(visitaNavioId, emitters);
        }
    }

    @Override
    public void destroy() {
        agendador.shutdownNow();
        assinantes.values().forEach(lista -> lista.forEach(SseEmitter::complete));
        assinantes.clear();
    }
}
