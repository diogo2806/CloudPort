package br.com.cloudport.servicogate.app.billing;

import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.CobrancaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaGeracaoRequest;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.PagamentoRequest;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.TarifaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.TarifaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing")
@Tag(name = "Billing", description = "Tarifas, cobranças, faturas e pagamentos operacionais")
public class BillingController {

    private final BillingCapService billingCapService;

    public BillingController(BillingCapService billingCapService) {
        this.billingCapService = billingCapService;
    }

    @GetMapping("/tarifas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    @Operation(summary = "Listar tarifas operacionais")
    public List<TarifaDTO> listarTarifas(@RequestParam(required = false) Boolean ativas) {
        return billingCapService.listarTarifas(ativas);
    }

    @PostMapping("/tarifas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Criar ou atualizar uma tarifa pelo código")
    public TarifaDTO salvarTarifa(@Valid @RequestBody TarifaRequest request) {
        return billingCapService.salvarTarifa(request);
    }

    @PostMapping("/cobrancas/agendamentos/{agendamentoId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    @Operation(summary = "Gerar cobrança idempotente para um atendimento concluído")
    public CobrancaDTO gerarCobranca(@PathVariable Long agendamentoId) {
        return billingCapService.gerarCobrancaAgendamento(agendamentoId);
    }

    @GetMapping("/cobrancas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    @Operation(summary = "Listar cobranças com isolamento automático da transportadora autenticada")
    public List<CobrancaDTO> listarCobrancas(
            @RequestParam(required = false) Long transportadoraId,
            @RequestParam(required = false) String status) {
        return billingCapService.listarCobrancas(transportadoraId, status);
    }

    @PostMapping("/faturas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Gerar fatura a partir de cobranças pendentes")
    public FaturaDTO gerarFatura(@Valid @RequestBody FaturaGeracaoRequest request) {
        return billingCapService.gerarFatura(request);
    }

    @GetMapping("/faturas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','TRANSPORTADORA')")
    @Operation(summary = "Listar faturas com isolamento automático da transportadora autenticada")
    public List<FaturaDTO> listarFaturas(
            @RequestParam(required = false) Long transportadoraId,
            @RequestParam(required = false) String status) {
        return billingCapService.listarFaturas(transportadoraId, status);
    }

    @PostMapping("/faturas/{faturaId}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Registrar pagamento e quitar automaticamente a fatura")
    public FaturaDTO registrarPagamento(@PathVariable Long faturaId,
                                        @Valid @RequestBody PagamentoRequest request) {
        return billingCapService.registrarPagamento(faturaId, request);
    }
}
