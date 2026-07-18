package br.com.cloudport.servicoyard.vesselplanner.controlador;

import br.com.cloudport.servicoyard.seguranca.PoliticaAutorizacaoEstiva;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.ComandoTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.CriarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.TampaResposta;
import br.com.cloudport.servicoyard.vesselplanner.servico.TampaPoraoServico;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vessel-planner/planos/{planId}/tampas-porao")
public class TampaPoraoControlador {

    private final TampaPoraoServico servico;

    public TampaPoraoControlador(TampaPoraoServico servico) {
        this.servico = servico;
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping
    public ResponseEntity<List<TampaResposta>> listar(@PathVariable Long planId) {
        return ResponseEntity.ok(servico.listar(planId));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/{tampaId}/tarefas")
    public ResponseEntity<TampaResposta> criarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tampaId,
            @Valid @RequestBody CriarTarefaRequest request,
            Principal principal) {
        TampaResposta resposta = servico.criarTarefa(planId, tampaId, request, usuario(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/tarefas/{tarefaId}/iniciar")
    public ResponseEntity<TampaResposta> iniciarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody(required = false) ComandoTarefaRequest request,
            Principal principal) {
        return ResponseEntity.ok(servico.iniciarTarefa(
                planId,
                tarefaId,
                request == null ? new ComandoTarefaRequest() : request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/tarefas/{tarefaId}/confirmar")
    public ResponseEntity<TampaResposta> confirmarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody(required = false) ComandoTarefaRequest request,
            Principal principal) {
        return ResponseEntity.ok(servico.confirmarTarefa(
                planId,
                tarefaId,
                request == null ? new ComandoTarefaRequest() : request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/tarefas/{tarefaId}/cancelar")
    public ResponseEntity<TampaResposta> cancelarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody(required = false) ComandoTarefaRequest request,
            Principal principal) {
        return ResponseEntity.ok(servico.cancelarTarefa(
                planId,
                tarefaId,
                request == null ? new ComandoTarefaRequest() : request,
                usuario(principal)));
    }

    private String usuario(Principal principal) {
        return principal == null ? "SISTEMA" : principal.getName();
    }
}
