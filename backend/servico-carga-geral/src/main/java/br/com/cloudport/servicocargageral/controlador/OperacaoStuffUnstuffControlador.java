package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.servico.OperacaoStuffUnstuffServico;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/operacoes-stuff-unstuff")
@Tag(name = "Stuff e unstuff", description = "Planejamento e execução transacional de estufagem e desova")
public class OperacaoStuffUnstuffControlador {

    private final OperacaoStuffUnstuffServico servico;

    public OperacaoStuffUnstuffControlador(OperacaoStuffUnstuffServico servico) {
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
    @Operation(summary = "Consultar operação de stuff ou unstuff")
    public OperacaoResposta obter(@PathVariable UUID id) {
        return servico.obter(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar ordem planejada de stuff ou unstuff")
    public ResponseEntity<OperacaoResposta> criar(@Valid @RequestBody CriarOperacaoRequest request) {
        OperacaoResposta criada = servico.criarOperacaoStuffUnstuff(request);
        return ResponseEntity.created(URI.create("/api/carga-geral/operacoes-stuff-unstuff/" + criada.id())).body(criada);
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Iniciar operação planejada")
    public OperacaoResposta iniciar(@PathVariable UUID id) {
        return servico.iniciar(id);
    }

    @PostMapping("/{id}/execucoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Registrar execução parcial, divergência ou avaria")
    public OperacaoResposta executar(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarExecucaoRequest request) {
        return servico.registrarExecucaoParcial(id, request);
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Concluir operação e registrar lacre final")
    public OperacaoResposta concluir(
            @PathVariable UUID id,
            @Valid @RequestBody ConcluirOperacaoRequest request) {
        return servico.concluir(id, request);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Cancelar operação com compensação dos saldos executados")
    public OperacaoResposta cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody CancelarOperacaoRequest request) {
        return servico.cancelar(id, request);
    }
}
