package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.ReconciliacaoBarcodeDTO;
import br.com.cloudport.servicogate.app.gestor.dto.ResolverDesincroniaRequest;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/reconciliacao")
@Tag(name = "Reconciliação de Barcode", description = "Monitoramento e reconciliação de desincronias")
public class ReconciliacaoBarcodeController {

    private final ReconciliacaoBarcodeService reconciliacaoService;

    public ReconciliacaoBarcodeController(ReconciliacaoBarcodeService reconciliacaoService) {
        this.reconciliacaoService = reconciliacaoService;
    }

    @PostMapping("/executar")
    @Operation(summary = "Executa reconciliação imediatamente (normalmente roda à noite)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<List<ReconciliacaoBarcodeDTO>> executarReconciliacao() {
        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();
        List<ReconciliacaoBarcodeDTO> dtos = resultado.stream()
                .map(ReconciliacaoBarcodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/nao-resolvidas")
    @Operation(summary = "Lista desincronias não resolvidas")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<List<ReconciliacaoBarcodeDTO>> listarNaoResolvidas() {
        List<ReconciliacaoBarcode> problemas = reconciliacaoService.listarNaoResolvidas();
        List<ReconciliacaoBarcodeDTO> dtos = problemas.stream()
                .map(ReconciliacaoBarcodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/por-tipo")
    @Operation(summary = "Lista desincronias por tipo")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<List<ReconciliacaoBarcodeDTO>> listarPorTipo(
            @RequestParam TipoDesincroniaBarcode tipo) {
        List<ReconciliacaoBarcode> problemas = reconciliacaoService.listarPorTipo(tipo);
        List<ReconciliacaoBarcodeDTO> dtos = problemas.stream()
                .map(ReconciliacaoBarcodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/resolver")
    @Operation(summary = "Marca desincronização como resolvida")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<Void> resolverDesinconia(
            @PathVariable Long id,
            @Valid @RequestBody ResolverDesincroniaRequest request) {
        reconciliacaoService.resolverDesinconia(id, request.getResolucao());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
