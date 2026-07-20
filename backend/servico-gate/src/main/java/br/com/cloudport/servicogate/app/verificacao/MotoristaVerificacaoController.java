package br.com.cloudport.servicogate.app.verificacao;

import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.CredencialMotoristaDTO;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.CredencialMotoristaRequest;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.OverrideVerificacaoMotoristaRequest;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.VerificacaoMotoristaDTO;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.VerificacaoMotoristaRequest;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/verificacoes-motorista")
@Tag(name = "Verificação do motorista", description = "Credenciais, tentativas, bloqueios e override operacional do Gate")
public class MotoristaVerificacaoController {

    private final MotoristaVerificacaoService service;

    public MotoristaVerificacaoController(MotoristaVerificacaoService service) {
        this.service = service;
    }

    @GetMapping("/visitas/{visitaId}")
    @Operation(summary = "Consulta a situação da verificação do motorista da truck visit")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public VerificacaoMotoristaDTO consultarVisita(@PathVariable Long visitaId) {
        return service.consultarVisita(visitaId);
    }

    @PostMapping("/visitas/{visitaId}/validar")
    @Operation(summary = "Valida PIN, documento ou credencial do motorista da truck visit")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public VerificacaoMotoristaDTO validarVisita(
            @PathVariable Long visitaId,
            @Valid @RequestBody VerificacaoMotoristaRequest request) {
        return service.validarVisita(visitaId, request);
    }

    @PostMapping("/visitas/{visitaId}/override")
    @Operation(summary = "Autoriza override motivado da verificação do motorista da truck visit")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public VerificacaoMotoristaDTO autorizarOverrideVisita(
            @PathVariable Long visitaId,
            @Valid @RequestBody OverrideVerificacaoMotoristaRequest request) {
        return service.autorizarOverrideVisita(visitaId, request);
    }

    @GetMapping("/agendamentos/{agendamentoId}")
    @Operation(summary = "Consulta a situação da verificação do motorista do agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public VerificacaoMotoristaDTO consultarAgendamento(@PathVariable Long agendamentoId) {
        return service.consultarAgendamento(agendamentoId);
    }

    @PostMapping("/agendamentos/{agendamentoId}/validar")
    @Operation(summary = "Valida PIN, documento ou credencial do motorista do agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public VerificacaoMotoristaDTO validarAgendamento(
            @PathVariable Long agendamentoId,
            @Valid @RequestBody VerificacaoMotoristaRequest request) {
        return service.validarAgendamento(agendamentoId, request);
    }

    @PostMapping("/agendamentos/{agendamentoId}/override")
    @Operation(summary = "Autoriza override motivado da verificação do motorista do agendamento")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public VerificacaoMotoristaDTO autorizarOverrideAgendamento(
            @PathVariable Long agendamentoId,
            @Valid @RequestBody OverrideVerificacaoMotoristaRequest request) {
        return service.autorizarOverrideAgendamento(agendamentoId, request);
    }

    @PostMapping("/motoristas/{motoristaId}/credenciais")
    @Operation(summary = "Cadastra ou substitui PIN ou credencial operacional do motorista")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<CredencialMotoristaDTO> cadastrarCredencial(
            @PathVariable Long motoristaId,
            @Valid @RequestBody CredencialMotoristaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.cadastrarCredencial(motoristaId, request));
    }
}
