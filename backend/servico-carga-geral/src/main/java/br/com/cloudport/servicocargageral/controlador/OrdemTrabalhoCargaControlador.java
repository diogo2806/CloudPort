package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.AtribuirRecursosRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.CancelarOrdemRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.CriarOrdemRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.OrdemResposta;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.RegistrarEventoRequest;
import br.com.cloudport.servicocargageral.servico.OrdemTrabalhoCargaServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/ordens-trabalho")
@Tag(name = "Ordens de trabalho de carga", description = "Planejamento, recursos e execução de serviços operacionais")
public class OrdemTrabalhoCargaControlador {
    private final OrdemTrabalhoCargaServico servico;
    public OrdemTrabalhoCargaControlador(OrdemTrabalhoCargaServico servico) { this.servico = servico; }

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    @Operation(summary = "Listar ordens de trabalho")
    public List<OrdemResposta> listar() { return servico.listar(); }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    @Operation(summary = "Consultar ordem de trabalho")
    public OrdemResposta obter(@PathVariable UUID id) { return servico.obter(id); }

    @PostMapping @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    @Operation(summary = "Criar ordem planejada")
    public ResponseEntity<OrdemResposta> criar(@Valid @RequestBody CriarOrdemRequest request, Principal principal) {
        OrdemResposta criada = servico.criar(request, usuario(principal));
        return ResponseEntity.created(URI.create("/api/carga-geral/ordens-trabalho/" + criada.id())).body(criada);
    }

    @PostMapping("/{id}/liberar") @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    @Operation(summary = "Liberar ordem para execução")
    public OrdemResposta liberar(@PathVariable UUID id, Principal principal) { return servico.liberar(id, usuario(principal)); }

    @PostMapping("/{id}/recursos") @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    @Operation(summary = "Atribuir equipe ou equipamento")
    public OrdemResposta atribuir(@PathVariable UUID id, @Valid @RequestBody AtribuirRecursosRequest request, Principal principal) { return servico.atribuir(id, request, usuario(principal)); }

    @PostMapping("/{id}/iniciar") @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    @Operation(summary = "Iniciar execução da ordem")
    public OrdemResposta iniciar(@PathVariable UUID id, Principal principal) { return servico.iniciar(id, usuario(principal)); }

    @PostMapping("/{id}/eventos") @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    @Operation(summary = "Registrar evento de serviço realizado")
    public OrdemResposta registrarEvento(@PathVariable UUID id, @Valid @RequestBody RegistrarEventoRequest request, Principal principal) { return servico.registrarEvento(id, request, usuario(principal)); }

    @PostMapping("/{id}/concluir") @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    @Operation(summary = "Concluir ordem em execução")
    public OrdemResposta concluir(@PathVariable UUID id, Principal principal) { return servico.concluir(id, usuario(principal)); }

    @PostMapping("/{id}/cancelar") @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    @Operation(summary = "Cancelar ordem com motivo")
    public OrdemResposta cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarOrdemRequest request, Principal principal) { return servico.cancelar(id, request, usuario(principal)); }

    private String usuario(Principal principal) { return principal == null ? "SISTEMA" : principal.getName(); }
}
