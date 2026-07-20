package br.com.cloudport.servicoyard.operacao2d;

import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.ComandoResponse;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.RegistrarComandoRequest;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.SalvarWorkspaceRequest;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.WorkspaceResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operacao-2d")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','OPERADOR_NAVIO','OPERADOR_FERROVIA')")
public class OperacaoGrafica2DControlador {

    private final OperacaoGrafica2DServico servico;

    public OperacaoGrafica2DControlador(OperacaoGrafica2DServico servico) {
        this.servico = servico;
    }

    @PostMapping("/comandos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','OPERADOR_NAVIO','OPERADOR_FERROVIA')")
    public ResponseEntity<ComandoResponse> registrarComando(
            @Valid @RequestBody RegistrarComandoRequest request,
            Authentication authentication) {
        ComandoResponse response = servico.registrarComando(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comandos/{commandId}")
    public ComandoResponse buscarComando(@PathVariable String commandId) {
        return servico.buscarComando(commandId);
    }

    @PostMapping("/workspaces")
    public ResponseEntity<WorkspaceResponse> salvarWorkspace(
            @Valid @RequestBody SalvarWorkspaceRequest request,
            Authentication authentication) {
        WorkspaceResponse response = servico.salvarWorkspace(
                request,
                authentication.getName(),
                papeis(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/workspaces")
    public List<WorkspaceResponse> listarWorkspaces(Authentication authentication) {
        return servico.listarWorkspaces(authentication.getName(), papeis(authentication));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErroDeNegocio(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    private Set<String> papeis(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
    }
}
