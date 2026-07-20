package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.OrdemLiberacaoStuffUnstuffDTOs.CriarOperacaoComLiberacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarProgramacaoDocaRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConfirmarPesagemStuffingRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarVersaoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.LacreOperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.LiberarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.PesagemStuffingResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.PlanoVersaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ProgramacaoDocaResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarLacreRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ReservarProgramacaoDocaRequest;
import br.com.cloudport.servicocargageral.servico.FluxoStuffUnstuffServico;
import br.com.cloudport.servicocargageral.servico.LacreStuffUnstuffServico;
import br.com.cloudport.servicocargageral.servico.PesagemStuffingServico;
import br.com.cloudport.servicocargageral.servico.PlanoStuffUnstuffServico;
import br.com.cloudport.servicocargageral.servico.ProgramacaoDocaCargaServico;
import br.com.cloudport.servicocargageral.servico.StuffUnstuffServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "Stuff e unstuff", description = "Planejamento versionado, autorização comercial, staging e execução parcial")
public class StuffUnstuffControlador {

    private final StuffUnstuffServico servico;
    private final PlanoStuffUnstuffServico planoServico;
    private final FluxoStuffUnstuffServico fluxoServico;
    private final LacreStuffUnstuffServico lacreServico;
    private final PesagemStuffingServico pesagemServico;
    private final ProgramacaoDocaCargaServico programacaoDocaServico;

    public StuffUnstuffControlador(
            StuffUnstuffServico servico,
            PlanoStuffUnstuffServico planoServico,
            FluxoStuffUnstuffServico fluxoServico,
            LacreStuffUnstuffServico lacreServico,
            PesagemStuffingServico pesagemServico,
            ProgramacaoDocaCargaServico programacaoDocaServico) {
        this.servico = servico;
        this.planoServico = planoServico;
        this.fluxoServico = fluxoServico;
        this.lacreServico = lacreServico;
        this.pesagemServico = pesagemServico;
        this.programacaoDocaServico = programacaoDocaServico;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar operações de stuff e unstuff")
    public List<OperacaoResposta> listar() { return servico.listar(); }

    @GetMapping("/programacoes-doca")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar agenda operacional de docas e staging")
    public List<ProgramacaoDocaResposta> listarProgramacoesDoca(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        return programacaoDocaServico.listar(inicio, fim);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar operação com itens e histórico")
    public OperacaoResposta obter(@PathVariable UUID id) { return servico.obter(id); }

    @GetMapping("/{id}/programacao-doca")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    public ProgramacaoDocaResposta obterProgramacaoDoca(@PathVariable UUID id) { return programacaoDocaServico.obter(id); }

    @GetMapping("/{id}/lacres")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    public List<LacreOperacaoResposta> listarLacres(@PathVariable UUID id) { return lacreServico.listar(id); }

    @GetMapping("/{id}/pesagem")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    public PesagemStuffingResposta obterPesagem(@PathVariable UUID id) { return pesagemServico.obter(id); }

    @GetMapping("/{id}/planos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    public List<PlanoVersaoResposta> listarPlanos(@PathVariable UUID id) { return planoServico.listar(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar operação reservando saldo de uma origem operacional válida")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Operação, origem e versão inicial criadas"),
        @ApiResponse(responseCode = "409", description = "Origem inválida, em hold, fora da vigência ou sem saldo")
    })
    public ResponseEntity<OperacaoResposta> criar(@Valid @RequestBody CriarOperacaoComLiberacaoRequest request) {
        CriarOperacaoRequest operacaoRequest = new CriarOperacaoRequest(
                request.tipo(), request.conteinerId(), request.armazemId(), request.posicaoOperacao(),
                request.equipeRecurso(), request.lacreInicial(), request.usuario(), request.correlationId(), request.itens());
        OperacaoResposta criada = fluxoServico.criar(operacaoRequest, request.origemOperacional());
        return ResponseEntity.created(URI.create("/api/carga-geral/operacoes-stuff-unstuff/" + criada.id())).body(criada);
    }

    @PostMapping("/{id}/planos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    public PlanoVersaoResposta criarVersao(@PathVariable UUID id, @Valid @RequestBody CriarVersaoPlanoRequest request) {
        return planoServico.criarNovaVersao(id, request);
    }

    @PostMapping("/{id}/planos/liberar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    public PlanoVersaoResposta liberarPlano(@PathVariable UUID id, @Valid @RequestBody LiberarPlanoRequest request) {
        return planoServico.liberar(id, request);
    }

    @PostMapping("/{id}/programacao-doca")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    public ProgramacaoDocaResposta reservarProgramacaoDoca(
            @PathVariable UUID id, @Valid @RequestBody ReservarProgramacaoDocaRequest request) {
        return programacaoDocaServico.reservar(id, request);
    }

    @PostMapping("/{id}/programacao-doca/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    public ProgramacaoDocaResposta cancelarProgramacaoDoca(
            @PathVariable UUID id, @Valid @RequestBody CancelarProgramacaoDocaRequest request) {
        return programacaoDocaServico.cancelar(id, request);
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    public OperacaoResposta iniciar(
            @PathVariable UUID id,
            @RequestParam @NotBlank @Size(max = 120) String usuario,
            @RequestParam(required = false) @Size(max = 120) String correlationId) {
        return fluxoServico.iniciar(id, usuario, correlationId);
    }

    @PostMapping("/{id}/execucoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    public OperacaoResposta registrarExecucao(
            @PathVariable UUID id, @Valid @RequestBody RegistrarExecucaoRequest request) {
        return fluxoServico.registrarExecucao(id, request);
    }

    @PostMapping("/{id}/lacres")
    @PreAuthorize("hasRole('ADMIN_PORTO') or (!#request.overrideAutorizado() and hasAnyRole('PLANEJADOR', 'OPERADOR_GATE'))")
    public LacreOperacaoResposta registrarLacre(@PathVariable UUID id, @Valid @RequestBody RegistrarLacreRequest request) {
        return lacreServico.registrar(id, request);
    }

    @PostMapping("/{id}/pesagem")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    public PesagemStuffingResposta confirmarPesagem(
            @PathVariable UUID id, @Valid @RequestBody ConfirmarPesagemStuffingRequest request) {
        return pesagemServico.confirmar(id, request);
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    public OperacaoResposta concluir(@PathVariable UUID id, @Valid @RequestBody ConcluirOperacaoRequest request) {
        return fluxoServico.concluir(id, request);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    public OperacaoResposta cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarOperacaoRequest request) {
        return fluxoServico.cancelar(id, request);
    }
}
