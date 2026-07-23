package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.dto.GrupoIsoEquipamentoDTO;
import br.com.cloudport.servicoyard.inventario.servico.GrupoIsoEquipamentoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/inventario/canonico/grupos-iso")
@Tag(name = "Grupos ISO", description = "Cadastro mestre de grupos ISO de equipamentos")
public class GrupoIsoEquipamentoControlador {

    private final GrupoIsoEquipamentoServico servico;

    public GrupoIsoEquipamentoControlador(GrupoIsoEquipamentoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar grupos ISO")
    public List<GrupoIsoEquipamentoDTO.Resposta> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean ativo) {
        return servico.listar(termo, categoria, ativo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhar grupo ISO")
    public GrupoIsoEquipamentoDTO.Resposta detalhar(@PathVariable Long id) {
        return servico.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Cadastrar grupo ISO")
    public GrupoIsoEquipamentoDTO.Resposta criar(
            @RequestBody GrupoIsoEquipamentoDTO.SalvarRequest request, Principal principal) {
        return servico.criar(request, usuario(principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Atualizar grupo ISO")
    public GrupoIsoEquipamentoDTO.Resposta atualizar(
            @PathVariable Long id,
            @RequestBody GrupoIsoEquipamentoDTO.SalvarRequest request,
            Principal principal) {
        return servico.atualizar(id, request, usuario(principal));
    }

    @PatchMapping("/{id}/situacao")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Ativar ou inativar grupo ISO")
    public GrupoIsoEquipamentoDTO.Resposta alterarSituacao(
            @PathVariable Long id, @RequestParam boolean ativo, Principal principal) {
        return servico.alterarSituacao(id, ativo, usuario(principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Excluir grupo ISO sem dependências")
    public void excluir(@PathVariable Long id) {
        servico.excluir(id);
    }

    private static String usuario(Principal principal) {
        return principal == null || principal.getName() == null ? "sistema" : principal.getName();
    }
}
