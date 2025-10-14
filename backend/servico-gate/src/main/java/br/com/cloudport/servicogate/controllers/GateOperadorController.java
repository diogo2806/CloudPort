package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.dto.operador.GateOperadorBloqueioRequest;
import br.com.cloudport.servicogate.dto.operador.GateOperadorEventoDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorLiberacaoRequest;
import br.com.cloudport.servicogate.dto.operador.GateOperadorOcorrenciaRequest;
import br.com.cloudport.servicogate.dto.operador.GateOperadorPainelDTO;
import br.com.cloudport.servicogate.service.GateOperadorOperacoesService;
import br.com.cloudport.servicogate.service.GateOperadorPainelService;
import br.com.cloudport.servicogate.service.GateOperadorRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/gate/operador")
@Tag(name = "Operador de Gate", description = "Painel operacional e ações de liberação/bloqueio do gate")
public class GateOperadorController {

    private final GateOperadorPainelService painelService;
    private final GateOperadorOperacoesService operacoesService;
    private final GateOperadorRealtimeService realtimeService;

    public GateOperadorController(GateOperadorPainelService painelService,
                                  GateOperadorOperacoesService operacoesService,
                                  GateOperadorRealtimeService realtimeService) {
        this.painelService = painelService;
        this.operacoesService = operacoesService;
        this.realtimeService = realtimeService;
    }

    @GetMapping("/painel")
    @Operation(summary = "Resumo atualizado do painel do operador do gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public GateOperadorPainelDTO obterPainel() {
        return painelService.montarPainel();
    }

    @GetMapping("/eventos")
    @Operation(summary = "Histórico recente de eventos operacionais do gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public List<GateOperadorEventoDTO> listarEventos() {
        return painelService.listarEventosRecentes(50);
    }

    @GetMapping(value = "/eventos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Canal em tempo real para eventos do gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public SseEmitter streamEventos() {
        return realtimeService.registrar();
    }

    @PostMapping("/veiculos/{veiculoId}/liberacao")
    @Operation(summary = "Liberação manual de veículo", description = "Registra liberação manual com justificativa")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<Void> liberarVeiculo(@PathVariable Long veiculoId,
                                               @Valid @RequestBody GateOperadorLiberacaoRequest request) {
        operacoesService.liberarVeiculo(veiculoId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/veiculos/{veiculoId}/bloqueio")
    @Operation(summary = "Bloqueio manual de veículo", description = "Registra bloqueio temporário com motivo")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<Void> bloquearVeiculo(@PathVariable Long veiculoId,
                                                @Valid @RequestBody GateOperadorBloqueioRequest request) {
        operacoesService.bloquearVeiculo(veiculoId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ocorrencias")
    @Operation(summary = "Registro de ocorrência operacional")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public ResponseEntity<Void> registrarOcorrencia(@Valid @RequestBody GateOperadorOcorrenciaRequest request) {
        operacoesService.registrarOcorrencia(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/veiculos/{veiculoId}/comprovante")
    @Operation(summary = "Gera comprovante textual do gate para o veículo informado")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public ResponseEntity<ByteArrayResource> imprimirComprovante(@PathVariable Long veiculoId) {
        var comprovante = operacoesService.gerarComprovante(veiculoId);
        ByteArrayResource resource = new ByteArrayResource(comprovante.conteudo());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(comprovante.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + comprovante.nomeArquivo() + "\"")
                .body(resource);
    }
}
