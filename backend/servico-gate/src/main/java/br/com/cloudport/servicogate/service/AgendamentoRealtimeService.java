package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.config.AgendamentoRulesProperties;
import br.com.cloudport.servicogate.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.dto.DocumentoAgendamentoDTO;
import br.com.cloudport.servicogate.dto.GatePassDTO;
import br.com.cloudport.servicogate.dto.mapper.GateMapper;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.repository.AgendamentoRepository;
import br.com.cloudport.servicogate.service.notification.NotificationGateway;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgendamentoRealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgendamentoRealtimeService.class);
    private static final Duration DEFAULT_SSE_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration DEFAULT_INTERVALO_REPETICAO = Duration.ofMinutes(5);

    private final AgendamentoRepository agendamentoRepository;
    private final NotificationGateway notificationGateway;
    private final AgendamentoRulesProperties rulesProperties;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emissoresPorAgendamento = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> ultimaJanelaNotificada = new ConcurrentHashMap<>();

    public AgendamentoRealtimeService(AgendamentoRepository agendamentoRepository,
                                      NotificationGateway notificationGateway,
                                      AgendamentoRulesProperties rulesProperties) {
        this.agendamentoRepository = agendamentoRepository;
        this.notificationGateway = notificationGateway;
        this.rulesProperties = rulesProperties;
    }

    public SseEmitter registrar(Long agendamentoId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT.toMillis());
        emissoresPorAgendamento.computeIfAbsent(agendamentoId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removerEmissor(agendamentoId, emitter));
        emitter.onTimeout(() -> removerEmissor(agendamentoId, emitter));
        emitter.onError(throwable -> removerEmissor(agendamentoId, emitter));

        agendamentoRepository.findById(agendamentoId)
                .ifPresent(agendamento -> {
                    enviar(emitter, "snapshot", GateMapper.toAgendamentoDTO(agendamento));
                    verificarJanelaProxima(agendamento);
                });
        return emitter;
    }

    public void notificarStatus(Agendamento agendamento) {
        AgendamentoDTO dto = GateMapper.toAgendamentoDTO(agendamento);
        enviar(agendamento.getId(), "status-atualizado", dto);
        notificationGateway.enviarStatusAtualizado(agendamento);
    }

    public void notificarGatePass(GatePass gatePass) {
        if (gatePass == null || gatePass.getAgendamento() == null) {
            return;
        }
        GatePassDTO dto = GateMapper.toGatePassDTO(gatePass);
        enviar(gatePass.getAgendamento().getId(), "gate-pass-atualizado", dto);
    }

    public void notificarDocumentosAtualizados(Agendamento agendamento) {
        List<DocumentoAgendamento> documentos = Optional.ofNullable(agendamento.getDocumentos())
                .orElse(List.of());
        List<DocumentoAgendamentoDTO> documentosDto = GateMapper.toDocumentoAgendamentoDTO(documentos);
        enviar(agendamento.getId(), "documentos-atualizados", documentosDto);
    }

    public void notificarDocumentosRevalidados(Agendamento agendamento) {
        List<DocumentoAgendamento> documentos = Optional.ofNullable(agendamento.getDocumentos())
                .orElse(List.of());
        List<DocumentoAgendamentoDTO> documentosDto = GateMapper.toDocumentoAgendamentoDTO(documentos);
        enviar(agendamento.getId(), "documentos-revalidados", documentosDto);
        notificationGateway.enviarDocumentosRevalidados(agendamento, documentos);
    }

    public void verificarJanelaProxima(Agendamento agendamento) {
        if (agendamento == null || agendamento.getHorarioPrevistoChegada() == null) {
            return;
        }
        Duration antecedenciaConfigurada = Objects.requireNonNullElse(
                rulesProperties.getNotificacaoJanelaAntecedencia(),
                Duration.ofMinutes(30));
        LocalDateTime agora = LocalDateTime.now();
        Duration ateJanela = Duration.between(agora, agendamento.getHorarioPrevistoChegada());
        if (ateJanela.isNegative()) {
            return;
        }
        if (ateJanela.compareTo(antecedenciaConfigurada) > 0) {
            return;
        }

        LocalDateTime ultimoEnvio = ultimaJanelaNotificada.get(agendamento.getId());
        if (ultimoEnvio != null && Duration.between(ultimoEnvio, agora).compareTo(DEFAULT_INTERVALO_REPETICAO) < 0) {
            return;
        }

        ultimaJanelaNotificada.put(agendamento.getId(), agora);
        JanelaProximaDTO payload = new JanelaProximaDTO(
                agendamento.getCodigo(),
                agendamento.getHorarioPrevistoChegada(),
                agendamento.getHorarioPrevistoSaida(),
                ateJanela.toMinutes()
        );
        enviar(agendamento.getId(), "janela-proxima", payload);
        notificationGateway.enviarJanelaProxima(agendamento, ateJanela);
    }

    private void enviar(Long agendamentoId, String evento, Object payload) {
        CopyOnWriteArrayList<SseEmitter> emissores = emissoresPorAgendamento.get(agendamentoId);
        if (emissores == null || emissores.isEmpty()) {
            return;
        }
        emissores.forEach(emitter -> enviar(emitter, evento, payload));
    }

    private void enviar(SseEmitter emitter, String evento, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(evento).data(payload));
        } catch (IOException ex) {
            LOGGER.debug("Falha ao enviar evento SSE", ex);
            emitter.completeWithError(ex);
        }
    }

    private void removerEmissor(Long agendamentoId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emissores = emissoresPorAgendamento.get(agendamentoId);
        if (emissores != null) {
            emissores.remove(emitter);
            if (emissores.isEmpty()) {
                emissoresPorAgendamento.remove(agendamentoId);
            }
        }
    }

    public record JanelaProximaDTO(String codigo,
                                   LocalDateTime horarioPrevistoChegada,
                                   LocalDateTime horarioPrevistoSaida,
                                   long minutosRestantes) {
    }
}
