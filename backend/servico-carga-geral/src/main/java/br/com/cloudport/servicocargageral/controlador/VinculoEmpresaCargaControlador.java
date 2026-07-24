package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.AtualizarVinculosRequest;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.VinculoEmpresaResposta;
import br.com.cloudport.servicocargageral.servico.VinculoEmpresaCargaServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral")
@Tag(name = "Empresas da carga geral", description = "Vínculos empresariais auditáveis de Bill of Lading e cargo lot")
public class VinculoEmpresaCargaControlador {

    private final VinculoEmpresaCargaServico servico;

    public VinculoEmpresaCargaControlador(VinculoEmpresaCargaServico servico) {
        this.servico = servico;
    }

    @GetMapping("/conhecimentos/{id}/empresas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar empresas vinculadas ao Bill of Lading")
    public List<VinculoEmpresaResposta> listarConhecimento(@PathVariable UUID id) {
        return servico.listarConhecimento(id);
    }

    @PutMapping("/conhecimentos/{id}/empresas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Substituir empresas vinculadas ao Bill of Lading")
    public List<VinculoEmpresaResposta> atualizarConhecimento(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarVinculosRequest request) {
        return servico.atualizarConhecimento(id, request);
    }

    @GetMapping("/lotes/{id}/empresas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar empresas vinculadas ao cargo lot")
    public List<VinculoEmpresaResposta> listarLote(@PathVariable UUID id) {
        return servico.listarLote(id);
    }

    @PutMapping("/lotes/{id}/empresas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Substituir empresas vinculadas ao cargo lot")
    public List<VinculoEmpresaResposta> atualizarLote(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarVinculosRequest request) {
        return servico.atualizarLote(id, request);
    }
}
