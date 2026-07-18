package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.TransloadResposta;
import br.com.cloudport.servicocargageral.servico.TransloadServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/transloads")
@Tag(name = "Transload de carga geral")
@PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
public class TransloadControlador {

    private final TransloadServico servico;

    public TransloadControlador(TransloadServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Executar transload atômico e recuperável entre unidades")
    public TransloadResposta executar(@Valid @RequestBody ExecutarTransloadRequest request) {
        return servico.executarTransload(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar rastreabilidade de uma operação de transload")
    public TransloadResposta obter(@PathVariable UUID id) {
        return servico.obter(id);
    }
}
