package br.com.cloudport.servicogate.app.integracao;

import br.com.cloudport.servicogate.app.gestor.GateFlowService;
import br.com.cloudport.servicogate.app.gestor.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.text.Normalizer;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/gate")
@Tag(name = "Integração com middleware de gate", description = "Recepção de eventos do middleware local do terminal")
public class GateWebhookController {

    private final GateFlowService gateFlowService;
    private final GateMetrics gateMetrics;

    public GateWebhookController(GateFlowService gateFlowService, GateMetrics gateMetrics) {
        this.gateFlowService = gateFlowService;
        this.gateMetrics = gateMetrics;
    }

    @PostMapping("/entrada")
    @Operation(summary = "Recebe evento de entrada enviado pelo middleware local do gate")
    @PreAuthorize("hasAuthority('SCOPE_gate.middleware') or hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<GateDecisionDTO> receberEntrada(@Valid @RequestBody GateFlowRequest requisicao) {
        return processarEvento(requisicao, true);
    }

    @PostMapping("/saida")
    @Operation(summary = "Recebe evento de saída enviado pelo middleware local do gate")
    @PreAuthorize("hasAuthority('SCOPE_gate.middleware') or hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<GateDecisionDTO> receberSaida(@Valid @RequestBody GateFlowRequest requisicao) {
        return processarEvento(requisicao, false);
    }

    private ResponseEntity<GateDecisionDTO> processarEvento(GateFlowRequest requisicaoOriginal, boolean entrada) {
        GateFlowRequest requisicao = sanitizarRequisicao(requisicaoOriginal);
        boolean sucesso = false;
        try {
            GateDecisionDTO decisao = entrada
                    ? gateFlowService.registrarEntrada(requisicao)
                    : gateFlowService.registrarSaida(requisicao);
            sucesso = true;
            return ResponseEntity.ok(decisao);
        } finally {
            gateMetrics.registrarEventoMiddleware(entrada ? "entrada" : "saida", sucesso);
        }
    }

    private GateFlowRequest sanitizarRequisicao(GateFlowRequest requisicao) {
        if (requisicao == null) {
            return null;
        }
        GateFlowRequest sanitizada = new GateFlowRequest();
        sanitizada.setPlaca(sanitizarTexto(requisicao.getPlaca()));
        sanitizada.setQrCode(sanitizarTexto(requisicao.getQrCode()));
        sanitizada.setOperador(sanitizarTexto(requisicao.getOperador()));
        sanitizada.setTimestamp(requisicao.getTimestamp());
        return sanitizada;
    }

    private String sanitizarTexto(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFKC);
        String semCaracteresPerigosos = normalizado.replaceAll("[<>\"'`]|&", "");
        return semCaracteresPerigosos.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "").trim();
    }
}
