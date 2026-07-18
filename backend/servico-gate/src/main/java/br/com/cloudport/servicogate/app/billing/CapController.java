package br.com.cloudport.servicogate.app.billing;

import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.CapResumoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cap")
@Tag(name = "CAP", description = "Portal de autosserviço da transportadora")
public class CapController {

    private final BillingCapService billingCapService;

    public CapController(BillingCapService billingCapService) {
        this.billingCapService = billingCapService;
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('TRANSPORTADORA')")
    @Operation(summary = "Consultar agendamentos, cobranças e faturas da transportadora autenticada")
    public CapResumoDTO consultarResumo() {
        return billingCapService.consultarCap();
    }
}
