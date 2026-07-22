package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.AmarradoResposta;
import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.CriarAmarradoRequest;
import br.com.cloudport.servicocargageral.servico.AmarradoCargaServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/amarrados")
@Tag(
        name = "Amarrados de carga geral",
        description = "Identificação e rastreabilidade de amarrados com uma ou várias referências")
public class AmarradoCargaControlador {

    private final AmarradoCargaServico amarradoCargaServico;

    public AmarradoCargaControlador(AmarradoCargaServico amarradoCargaServico) {
        this.amarradoCargaServico = amarradoCargaServico;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE', 'OPERADOR_PATIO')")
    @Operation(summary = "Registrar amarrado e identificar automaticamente referências de grupos distintos")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Amarrado registrado"),
        @ApiResponse(responseCode = "400", description = "Referências duplicadas ou de outra visita"),
        @ApiResponse(responseCode = "404", description = "Cargo lot não encontrado"),
        @ApiResponse(responseCode = "409", description = "Código ou cargo lot já vinculado")
    })
    public ResponseEntity<AmarradoResposta> criar(@Valid @RequestBody CriarAmarradoRequest request) {
        AmarradoResposta criado = amarradoCargaServico.criar(request);
        return ResponseEntity
                .created(URI.create("/api/carga-geral/amarrados/" + criado.id()))
                .body(criado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE', 'OPERADOR_PATIO')")
    @Operation(summary = "Listar amarrados, com filtro por visita de navio ou cargo lot")
    public List<AmarradoResposta> listar(
            @RequestParam(required = false) String visitaNavioId,
            @RequestParam(required = false) UUID loteId) {
        return amarradoCargaServico.listar(visitaNavioId, loteId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE', 'OPERADOR_PATIO')")
    @Operation(summary = "Consultar amarrado com todas as referências vinculadas")
    @ApiResponse(responseCode = "404", description = "Amarrado não encontrado")
    public AmarradoResposta obter(@PathVariable UUID id) {
        return amarradoCargaServico.obter(id);
    }
}
