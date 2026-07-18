package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarVersaoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.LiberarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.PlanoVersaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.servico.FluxoStuffUnstuffServico;
import br.com.cloudport.servicocargageral.servico.PlanoStuffUnstuffServico;
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
@Tag(name = "Stuff e unstuff", description = "Planejamento versionado, execução parcial e encerramento de estufagem e desova")
public class StuffUnstuffControlador {

    private final StuffUnstuffServico servico;
    private final PlanoStuffUnstuffServico planoServico;
    private final FluxoStuffUnstuffServico fluxoServico;

    public StuffUnstuffControlador(
            StuffUnstuffServico servico,
            PlanoStuffUnstuffServico planoServico,
            FluxoStuffUnstuffServico fluxoServico) {
        this.servico = servico;
        this.planoServico = planoServico;
        this.fluxoServico = fluxoServico;
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

    @GetMapping("/{id}/planos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar versões imutáveis do plano")
    public List<PlanoVersaoResposta> listarPlanos(@PathVariable UUID id) {
        return planoServico.listar(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar ordem planejada de stuff ou unstuff com versão inicial")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Operação e versão inicial criadas"),
        @ApiResponse(responseCode = "409", description = "Planejamento incompatível com saldo ou capacidade")
    })
    public ResponseEntity<OperacaoResposta> criar(@Valid @RequestBody CriarOperacaoRequest request) {
        OperacaoResposta criada = fluxoServico.criar(request);
        return ResponseEntity.created(URI.create("/api/carga-geral/operacoes-stuff-unstuff/" + criada.id())).body(criada);
    }

    @PostMapping("/{id}/planos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar nova versão imutável do plano antes da execução")
    public PlanoVersaoResposta criarVersao(
            @PathVariable UUID id,
            @Valid @RequestBody CriarVersaoPlanoRequest request) {
        return planoServico.criarNovaVersao(id, request);
    }

    @PostMapping("/{id}/planos/liberar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Validar capacidade e liberar a versão mais recente para execução física")
    public PlanoVersaoResposta liberarPlano(
            @PathVariable UUID id,
            @Valid @RequestBody LiberarPlanoRequest request) {
        return planoServico.liberar(id, request);
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Iniciar operação com plano liberado")
    public OperacaoResposta iniciar(
            @PathVariable UUID id,
            @RequestParam @NotBlank @Size(max = 120) String usuario,
            @RequestParam(required = false) @Size(max = 120) String correlationId) {
        return fluxoServico.iniciar(id, usuario, correlationId);
    }

    @PostMapping("/{id}/execucoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Registrar execução parcial idempotente de um item do plano liberado")
    public OperacaoResposta registrarExecucao(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarExecucaoRequest request) {
        return fluxoServico.registrarExecucao(id, request);
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
