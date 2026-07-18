package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.modelo.EventoInstrucaoVmt;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import br.com.cloudport.servicoyard.patio.servico.EventoInstrucaoVmtServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard/work-instructions/{instructionId}/vmt-events")
@Tag(name = "Eventos VMT", description = "Confirmações idempotentes e ordenadas de instruções de trabalho")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO','PLANEJADOR','INTEGRACAO_VMT')")
public class EventoInstrucaoVmtController {

    private final EventoInstrucaoVmtServico servico;

    public EventoInstrucaoVmtController(EventoInstrucaoVmtServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Processa aceite, início, falha ou conclusão enviados pelo VMT")
    public ResponseEntity<Map<String, Object>> processar(@PathVariable Long instructionId,
                                                          @Valid @RequestBody EventoVmtRequest request) {
        EventoInstrucaoVmt evento = servico.processar(
                request.getEventId(), instructionId, request.getTipoEvento(), request.getStatusEsperado(),
                request.getTimestamp(), request.getResultado(), request.getPayload());
        return ResponseEntity.ok(mapear(evento));
    }

    @GetMapping
    @Operation(summary = "Lista o ciclo persistido de eventos VMT")
    public List<Map<String, Object>> listar(@PathVariable Long instructionId) {
        return servico.listar(instructionId).stream().map(this::mapear).collect(Collectors.toList());
    }

    private Map<String, Object> mapear(EventoInstrucaoVmt evento) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", evento.getId());
        dto.put("eventId", evento.getEventId());
        dto.put("instructionId", evento.getInstrucao().getId());
        dto.put("tipoEvento", evento.getTipoEvento());
        dto.put("statusEsperado", evento.getStatusEsperado());
        dto.put("timestamp", evento.getOcorridoEm());
        dto.put("resultado", evento.getResultado());
        dto.put("payload", evento.getPayload());
        dto.put("processadoEm", evento.getProcessadoEm());
        return dto;
    }

    public static class EventoVmtRequest {
        @NotBlank @Size(max = 120) private String eventId;
        @NotNull private TipoEventoVmt tipoEvento;
        @NotNull private StatusInstrucao statusEsperado;
        @NotNull private LocalDateTime timestamp;
        @Size(max = 1000) private String resultado;
        private String payload;
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public TipoEventoVmt getTipoEvento() { return tipoEvento; }
        public void setTipoEvento(TipoEventoVmt tipoEvento) { this.tipoEvento = tipoEvento; }
        public StatusInstrucao getStatusEsperado() { return statusEsperado; }
        public void setStatusEsperado(StatusInstrucao statusEsperado) { this.statusEsperado = statusEsperado; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getResultado() { return resultado; }
        public void setResultado(String resultado) { this.resultado = resultado; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
