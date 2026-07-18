package br.com.cloudport.servicogate.app.operacional;

import br.com.cloudport.servicogate.app.operacional.dto.GateComplementarDtos.GateComplementarDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateComplementarDtos.VinculoBillOfLadingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AccessRuleDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AccessRuleRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BillOfLadingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BillOfLadingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/operacional")
@Tag(name = "Gate operacional complementar", description = "Bill of Lading e regras de motorista, transportadora e veículo")
public class GateComplementarController {

    private final GateComplementarService service;

    public GateComplementarController(GateComplementarService service) {
        this.service = service;
    }

    @GetMapping("/complementos")
    @Operation(summary = "Lista Bills of Lading e regras de acesso do Gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public GateComplementarDTO listar(
            @Parameter(description = "Instalação usada para filtrar regras")
            @RequestParam(required = false) Long facilityId) {
        return service.listar(facilityId);
    }

    @PostMapping("/bills-of-lading")
    @Operation(summary = "Cria ou atualiza um Bill of Lading")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public ResponseEntity<BillOfLadingDTO> salvarBillOfLading(
            @Valid @RequestBody BillOfLadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvarBillOfLading(request));
    }

    @PostMapping("/ordens/{ordemId}/bill-of-lading/{billOfLadingId}")
    @Operation(summary = "Vincula EDO, ERO ou IDO a um Bill of Lading")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public VinculoBillOfLadingDTO vincularBillOfLading(@PathVariable Long ordemId,
                                                       @PathVariable Long billOfLadingId) {
        return service.vincularBillOfLading(ordemId, billOfLadingId);
    }

    @PostMapping("/configuracao/regras-acesso")
    @Operation(summary = "Cria ou atualiza regra de motorista, transportadora ou veículo")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<AccessRuleDTO> salvarRegra(
            @Valid @RequestBody AccessRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvarRegra(request));
    }
}