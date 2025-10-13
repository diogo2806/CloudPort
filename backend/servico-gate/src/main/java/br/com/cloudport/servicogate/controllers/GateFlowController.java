package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.dto.GateEventDTO;
import br.com.cloudport.servicogate.dto.GateFlowRequest;
import br.com.cloudport.servicogate.dto.ManualReleaseRequest;
import br.com.cloudport.servicogate.dto.TosSyncResponse;
import br.com.cloudport.servicogate.service.GateFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate")
@Tag(name = "Fluxo de Gate", description = "Processamento de eventos de entrada e saída do gate")
public class GateFlowController {

    private final GateFlowService gateFlowService;

    public GateFlowController(GateFlowService gateFlowService) {
        this.gateFlowService = gateFlowService;
    }

    @PostMapping("/entrada")
    @Operation(summary = "Processa um evento de entrada identificado por placa ou QR code")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<GateDecisionDTO> registrarEntrada(@Valid @RequestBody GateFlowRequest request) {
        GateDecisionDTO decision = gateFlowService.registrarEntrada(request);
        return ResponseEntity.ok(decision);
    }

    @PostMapping("/saida")
    @Operation(summary = "Processa um evento de saída identificado por placa ou QR code")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<GateDecisionDTO> registrarSaida(@Valid @RequestBody GateFlowRequest request) {
        GateDecisionDTO decision = gateFlowService.registrarSaida(request);
        return ResponseEntity.ok(decision);
    }

    @PostMapping("/agendamentos/{id}/liberacao-manual")
    @Operation(summary = "Registra liberação ou bloqueio manual para um agendamento")
    @PreAuthorize("hasRole('OPERADOR_GATE')")
    public ResponseEntity<GateEventDTO> liberarManual(@PathVariable("id") Long agendamentoId,
                                                      @Valid @RequestBody ManualReleaseRequest request) {
        GateEventDTO evento = gateFlowService.liberarManual(agendamentoId, request);
        return ResponseEntity.ok(evento);
    }

    @PostMapping("/agendamentos/{id}/sincronizar")
    @Operation(summary = "Força a ressincronização das informações do agendamento com o TOS")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public ResponseEntity<TosSyncResponse> sincronizar(@PathVariable("id") Long agendamentoId) {
        TosSyncResponse sincronizacao = gateFlowService.sincronizarAgendamento(agendamentoId);
        return ResponseEntity.ok(sincronizacao);
    }

    @PostMapping("/agendamentos/{id}/confirmar-chegada")
    @Operation(summary = "Confirma chegada antecipada do motorista antes da janela")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public ResponseEntity<AgendamentoDTO> confirmarChegada(@PathVariable("id") Long agendamentoId) {
        AgendamentoDTO atualizado = gateFlowService.confirmarChegadaAntecipada(agendamentoId);
        return ResponseEntity.ok(atualizado);
    }
}
