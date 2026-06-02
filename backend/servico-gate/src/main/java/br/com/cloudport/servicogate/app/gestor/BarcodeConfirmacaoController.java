package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest;
import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/barcode")
@Tag(name = "Confirmação de Barcode", description = "Webhook para validação de barcode via DMT")
public class BarcodeConfirmacaoController {

    private final ConfirmacaoBarcodeService confirmacaoBarcodeService;

    public BarcodeConfirmacaoController(ConfirmacaoBarcodeService confirmacaoBarcodeService) {
        this.confirmacaoBarcodeService = confirmacaoBarcodeService;
    }

    @PostMapping("/confirmar")
    @Operation(
        summary = "Recebe confirmação de barcode do dispositivo DMT",
        description = "Webhook chamado pelo DMT quando operador confirma ou rejeita a leitura do barcode"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ConfirmacaoBarcodeResponse> confirmarBarcode(
            @Valid @RequestBody ConfirmacaoBarcodeRequest request) {
        ConfirmacaoBarcodeResponse resposta = confirmacaoBarcodeService.confirmarBarcode(request);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/timeout")
    @Operation(
        summary = "Notifica timeout na confirmação de barcode",
        description = "Chamado pelo sistema de gate se o DMT não responder no tempo esperado"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<ConfirmacaoBarcodeResponse> registrarTimeout(
            @Valid @RequestBody TimeoutBarcodeRequest request) {
        ConfirmacaoBarcodeResponse resposta = confirmacaoBarcodeService
                .registrarTimeoutBarcode(request.getTokenGatePass(), request.getDispositivoDmtId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resposta);
    }
}
