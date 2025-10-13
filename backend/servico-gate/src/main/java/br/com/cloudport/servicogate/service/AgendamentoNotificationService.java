package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.dto.AgendamentoStatusEventDTO;
import br.com.cloudport.servicogate.dto.DocumentoRevalidacaoResultadoDTO;
import br.com.cloudport.servicogate.dto.JanelaLembreteDTO;
import br.com.cloudport.servicogate.dto.mapper.GateMapper;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgendamentoNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgendamentoNotificationService.class);
    private static final long STREAM_TIMEOUT = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Set<SseEmitter>> emittersPorAgendamento = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> lembretesDisparados = new ConcurrentHashMap<>();

    public SseEmitter conectar(Long agendamentoId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT);
        emittersPorAgendamento.computeIfAbsent(agendamentoId, id -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> removerEmitter(agendamentoId, emitter));
        emitter.onTimeout(() -> removerEmitter(agendamentoId, emitter));
        emitter.onError((ex) -> removerEmitter(agendamentoId, emitter));
        return emitter;
    }

    public void publicarStatusAtualizado(Agendamento agendamento) {
        AgendamentoStatusEventDTO evento = new AgendamentoStatusEventDTO(
                agendamento.getId(),
                agendamento.getStatus().name(),
                agendamento.getStatus().getDescricao(),
                agendamento.getHorarioRealChegada(),
                agendamento.getHorarioRealSaida(),
                agendamento.getObservacoes()
        );
        enviarEvento(agendamento.getId(), SseEmitter.event().name("status").data(evento));
    }

    public void publicarDocumentosRevalidados(Agendamento agendamento,
                                               List<DocumentoRevalidacaoResultadoDTO> resultados) {
        enviarEvento(agendamento.getId(), SseEmitter.event().name("documentos-revalidados").data(resultados));
    }

    public void publicarLembreteJanela(Agendamento agendamento) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime ultimoLembrete = lembretesDisparados.get(agendamento.getId());
        if (ultimoLembrete != null && Duration.between(ultimoLembrete, agora).toMinutes() < 5) {
            return;
        }
        lembretesDisparados.put(agendamento.getId(), agora);
        long minutosRestantes = Duration.between(agora, agendamento.getHorarioPrevistoChegada()).toMinutes();
        JanelaLembreteDTO dto = new JanelaLembreteDTO(
                agendamento.getId(),
                agendamento.getCodigo(),
                agendamento.getHorarioPrevistoChegada(),
                agendamento.getHorarioPrevistoSaida(),
                minutosRestantes
        );
        enviarEvento(agendamento.getId(), SseEmitter.event().name("window-reminder").data(dto));
    }

    public void publicarResumoStatus(Agendamento agendamento) {
        enviarEvento(agendamento.getId(), SseEmitter.event().name("snapshot").data(GateMapper.toAgendamentoDTO(agendamento)));
    }

    public Set<StatusAgendamento> statusesElegiveisParaLembrete() {
        return Collections.unmodifiableSet(EnumSet.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO));
    }

    private void removerEmitter(Long agendamentoId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersPorAgendamento.getOrDefault(agendamentoId, Collections.emptySet());
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersPorAgendamento.remove(agendamentoId);
        }
    }

    private void enviarEvento(Long agendamentoId, SseEmitter.SseEventBuilder evento) {
        Set<SseEmitter> emitters = emittersPorAgendamento.get(agendamentoId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        List<SseEmitter> falhos = emitters.stream().filter(emitter -> !emitEvento(emitter, evento)).collect(Collectors.toList());
        falhos.forEach(emitter -> removerEmitter(agendamentoId, emitter));
    }

    private boolean emitEvento(SseEmitter emitter, SseEmitter.SseEventBuilder evento) {
        try {
            emitter.send(evento);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Falha ao enviar evento SSE", e);
            return false;
        }
    }
}
