package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.servico.StuffUnstuffServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/operacoes-stuff-unstuff")
@Validated
@Tag(name = "Stuff e unstuff", description = "Planejamento, execução parcial e encerramento de estufagem e desova")
public class StuffUnstuffControlador {

    private final StuffUnstuffServico servico;

    public StuffUnstuffControlador(StuffUnstuffServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar operações de stuff e unstuff")
    public List<OperacaoResposta> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar operação com itens e histórico")
    public OperacaoResposta obter(@PathVariable UUID id) {
        return servico.obter(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar ordem planejada de stuff ou unstuff")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Operação criada"),
        @ApiResponse(responseCode = "409", description = "Planejamento incompatível com saldo ou capacidade")
    })
    public ResponseEntity<OperacaoResposta> criar(@Valid @RequestBody CriarOperacaoRequest request) {
        OperacaoResposta criada = servico.criarOperacaoStuffUnstuff(request);
        return ResponseEntity.created(URI.create("/api/carga-geral/operacoes-stuff-unstuff/" + criada.id())).body(criada);
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Iniciar operação planejada")
    public OperacaoResposta iniciar(
            @PathVariable UUID id,
            @RequestParam @NotBlank @Size(max = 120) String usuario,
            @RequestParam(required = false) @Size(max = 120) String correlationId) {
        return servico.iniciar(id, usuario, correlationId);
    }

    @PostMapping("/{id}/execucoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Registrar execução parcial, divergência e avaria")
    public OperacaoResposta registrarExecucao(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarExecucaoRequest request) {
        return servico.registrarExecucao(id, request);
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Concluir operação integralmente executada")
    public OperacaoResposta concluir(
            @PathVariable UUID id,
            @Valid @RequestBody ConcluirOperacaoRequest request) {
        return servico.concluir(id, request);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Cancelar operação e compensar saldos já executados")
    public OperacaoResposta cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody CancelarOperacaoRequest request) {
        return servico.cancelar(id, request);
    }
}
