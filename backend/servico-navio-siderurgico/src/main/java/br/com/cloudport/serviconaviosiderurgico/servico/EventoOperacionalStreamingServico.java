package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoOperacionalVersionadoDTO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EventoOperacionalStreamingServico {

    private static final String SCHEMA_VERSION = "1.0";
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final AtomicLong sequencia = new AtomicLong();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> assinantesPorVisita = new ConcurrentHashMap<>();

    public SseEmitter assinar(Long visitaNavioId, String ultimoEventoId) {
        SseEmitter emissor = new SseEmitter(TIMEOUT_MILLIS);
        CopyOnWriteArrayList<SseEmitter> assinantes = assinantesPorVisita.computeIfAbsent(
                visitaNavioId,
                chave -> new CopyOnWriteArrayList<>()
        );
        assinantes.add(emissor);
        emissor.onCompletion(() -> remover(visitaNavioId, emissor));
        emissor.onTimeout(() -> remover(visitaNavioId, emissor));
        emissor.onError(erro -> remover(visitaNavioId, emissor));

        EventoOperacionalVersionadoDTO inicial = novoEvento(
                visitaNavioId,
                null,
                "VISITA_NAVIO",
                "STREAM_CONECTADO",
                "sistema",
                Map.of(
                        "ultimoEventoIdRecebido", ultimoEventoId == null ? "" : ultimoEventoId,
                        "requerSnapshot", true
                )
        );
        enviar(visitaNavioId, emissor, inicial);
        return emissor;
    }

    public void publicar(EventoVisitaNavio evento) {
        if (evento == null || evento.getVisitaNavio() == null || evento.getVisitaNavio().getId() == null) {
            return;
        }
        EventoOperacionalVersionadoDTO versao = novoEvento(
                evento.getVisitaNavio().getId(),
                evento.getItemOperacao() == null ? null : evento.getItemOperacao().getId(),
                evento.getItemOperacao() == null ? "VISITA_NAVIO" : "ITEM_OPERACAO_NAVIO",
                evento.getTipoEvento(),
                evento.getUsuario(),
                Map.of(
                        "descricao", valor(evento.getDescricao()),
                        "dadosAntes", valor(evento.getDadosAntes()),
                        "dadosDepois", valor(evento.getDadosDepois()),
                        "eventoPersistidoId", evento.getId() == null ? "" : evento.getId()
                )
        );
        publicar(versao);
    }

    public void publicar(Long visitaNavioId, String agregado, String tipoEvento, Map<String, Object> dados) {
        publicar(novoEvento(visitaNavioId, null, agregado, tipoEvento, "sistema", dados));
    }

    private void publicar(EventoOperacionalVersionadoDTO evento) {
        List<SseEmitter> assinantes = assinantesPorVisita.getOrDefault(
                evento.visitaNavioId(),
                new CopyOnWriteArrayList<>()
        );
        assinantes.forEach(emissor -> enviar(evento.visitaNavioId(), emissor, evento));
    }

    private EventoOperacionalVersionadoDTO novoEvento(
            Long visitaNavioId,
            Long itemOperacaoNavioId,
            String agregado,
            String tipoEvento,
            String usuario,
            Map<String, Object> dados
    ) {
        long proximaSequencia = sequencia.incrementAndGet();
        String eventId = visitaNavioId + "-" + proximaSequencia + "-" + UUID.randomUUID();
        return new EventoOperacionalVersionadoDTO(
                SCHEMA_VERSION,
                proximaSequencia,
                eventId,
                visitaNavioId,
                itemOperacaoNavioId,
                agregado,
                tipoEvento,
                LocalDateTime.now(),
                usuario == null || usuario.isBlank() ? "sistema" : usuario,
                MDC.get("correlationId"),
                dados == null ? Map.of() : Map.copyOf(dados)
        );
    }

    private void enviar(Long visitaNavioId, SseEmitter emissor, EventoOperacionalVersionadoDTO evento) {
        try {
            emissor.send(SseEmitter.event()
                    .id(evento.eventId())
                    .name("cloudport.operacao.v1")
                    .reconnectTime(2000L)
                    .data(evento));
        } catch (IOException | IllegalStateException erro) {
            remover(visitaNavioId, emissor);
            emissor.completeWithError(erro);
        }
    }

    private void remover(Long visitaNavioId, SseEmitter emissor) {
        CopyOnWriteArrayList<SseEmitter> assinantes = assinantesPorVisita.get(visitaNavioId);
        if (assinantes == null) {
            return;
        }
        assinantes.remove(emissor);
        if (assinantes.isEmpty()) {
            assinantesPorVisita.remove(visitaNavioId, assinantes);
        }
    }

    private Object valor(Object valor) {
        return valor == null ? "" : valor;
    }
}
