package br.com.cloudport.servicogate.app.cidadao.centralacao;

import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.CentralAcaoAgendamentoRespostaDTO;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.VisaoCompletaAgendamentoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/agendamentos")
@Validated
@Tag(name = "Central de Ação", description = "Visão consolidada dos agendamentos para atuação rápida")
public class CentralAcaoAgendamentoController {

    private final CentralAcaoAgendamentoService centralAcaoAgendamentoService;

    public CentralAcaoAgendamentoController(CentralAcaoAgendamentoService centralAcaoAgendamentoService) {
        this.centralAcaoAgendamentoService = centralAcaoAgendamentoService;
    }

    @GetMapping("/visao-completa")
    @Operation(summary = "Lista cartões de ação dos agendamentos com dados consolidados")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public CentralAcaoAgendamentoRespostaDTO listarVisaoCompleta(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return centralAcaoAgendamentoService.montarVisaoCompleta(authorizationHeader);
    }

    @GetMapping("/{id}/visao-completa")
    @Operation(summary = "Detalha um agendamento com visão consolidada para ação imediata")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public VisaoCompletaAgendamentoDTO buscarPorId(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return centralAcaoAgendamentoService.montarVisaoPorId(id, authorizationHeader);
    }
}
