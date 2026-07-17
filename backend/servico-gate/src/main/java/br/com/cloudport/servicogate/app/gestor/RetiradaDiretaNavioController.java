package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioDTO;
import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/retiradas-diretas-navio")
@Tag(name = "Retirada direta do navio", description = "Saída de carga autopropelida descarregada do navio")
public class RetiradaDiretaNavioController {

    private final RetiradaDiretaNavioService service;

    public RetiradaDiretaNavioController(RetiradaDiretaNavioService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registra a saída direta pelo gate de carga autopropelida descarregada do navio")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<RetiradaDiretaNavioDTO> processar(
            @Valid @RequestBody RetiradaDiretaNavioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.processar(request));
    }

    @GetMapping
    @Operation(summary = "Lista as retiradas diretas do navio registradas no gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public ResponseEntity<Page<RetiradaDiretaNavioDTO>> listar(
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }
}
