package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.servico.GrupoIsoAssociacaoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/inventario/canonico/tipos/{tipoId}/grupo-iso")
@Tag(name = "Grupos ISO", description = "Vínculo entre tipos de equipamento e grupos ISO")
public class GrupoIsoAssociacaoControlador {

    private final GrupoIsoAssociacaoServico servico;

    public GrupoIsoAssociacaoControlador(GrupoIsoAssociacaoServico servico) {
        this.servico = servico;
    }

    @PutMapping("/{grupoIsoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Associar Grupo ISO a um tipo de equipamento")
    public void associar(@PathVariable Long tipoId, @PathVariable Long grupoIsoId) {
        servico.associar(tipoId, grupoIsoId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Remover associação de Grupo ISO")
    public void desassociar(@PathVariable Long tipoId) {
        servico.desassociar(tipoId);
    }
}
