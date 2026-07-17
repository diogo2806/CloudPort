package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.EmbarqueDiretoNavioRequest;
import br.com.cloudport.servicogate.app.gestor.dto.EmbarqueDiretoNavioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/embarques-diretos")
@Tag(name = "Embarque direto", description = "Fluxos que seguem do gate ao cais sem passagem pelo pátio")
public class EmbarqueDiretoNavioController {

    private final EmbarqueDiretoNavioService embarqueDiretoNavioService;

    public EmbarqueDiretoNavioController(EmbarqueDiretoNavioService embarqueDiretoNavioService) {
        this.embarqueDiretoNavioService = embarqueDiretoNavioService;
    }

    @PostMapping("/navio")
    @Operation(summary = "Confirma o embarque de um contêiner diretamente do gate para o navio")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<EmbarqueDiretoNavioResponse> embarcar(
            @Valid @RequestBody EmbarqueDiretoNavioRequest request) {
        return ResponseEntity.ok(embarqueDiretoNavioService.embarcar(request));
    }
}
