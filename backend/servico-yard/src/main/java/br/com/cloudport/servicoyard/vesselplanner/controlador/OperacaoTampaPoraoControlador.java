package br.com.cloudport.servicoyard.vesselplanner.controlador;

import br.com.cloudport.servicoyard.seguranca.PoliticaAutorizacaoEstiva;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.CancelarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.ConfirmarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.ConcluirMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.IniciarMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.IniciarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.MovimentoResposta;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.TampaPoraoResposta;
import br.com.cloudport.servicoyard.vesselplanner.servico.OperacaoTampaPoraoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/vessel-planner/planos/{planId}")
@Tag(
        name = "Operação de tampas de porão",
        description = "Planejamento, dependências, confirmação e bloqueio operacional das tampas")
public class OperacaoTampaPoraoControlador {

    private final OperacaoTampaPoraoServico servico;

    public OperacaoTampaPoraoControlador(OperacaoTampaPoraoServico servico) {
        this.servico = servico;
    }

    @GetMapping("/tampas-porao")
    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @Operation(summary = "Listar tampas e tarefas persistidas do plano")
    public List<TampaPoraoResposta> listar(@PathVariable Long planId) {
        return servico.listar(planId);
    }

    @PostMapping("/tampas-porao/sincronizar")
    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @Operation(summary = "Gerar tampas e tarefas dependentes a partir da geometria do plano")
    public ResponseEntity<List<TampaPoraoResposta>> sincronizar(@PathVariable Long planId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.sincronizar(planId));
    }

    @PostMapping("/tampas-porao/tarefas/{tarefaId}/iniciar")
    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @Operation(summary = "Iniciar tarefa de abertura, remoção, posicionamento ou fechamento")
    public TampaPoraoResposta iniciarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody IniciarTarefaRequest request,
            Principal principal) {
        return servico.iniciarTarefa(planId, tarefaId, request, usuario(principal));
    }

    @PostMapping("/tampas-porao/tarefas/{tarefaId}/confirmar")
    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @Operation(summary = "Confirmar tarefa e liberar a dependência seguinte")
    public TampaPoraoResposta confirmarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody ConfirmarTarefaRequest request,
            Principal principal) {
        return servico.confirmarTarefa(planId, tarefaId, request, usuario(principal));
    }

    @PostMapping("/tampas-porao/tarefas/{tarefaId}/cancelar")
    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @Operation(summary = "Cancelar tarefa com motivo auditável")
    public TampaPoraoResposta cancelarTarefa(
            @PathVariable Long planId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody CancelarTarefaRequest request,
            Principal principal) {
        return servico.cancelarTarefa(
                planId,
                tarefaId,
                request.getMotivo(),
                usuario(principal));
    }

    @GetMapping("/movimentos-operacionais")
    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @Operation(summary = "Listar movimentos de contêiner iniciados no plano")
    public List<MovimentoResposta> listarMovimentos(@PathVariable Long planId) {
        return servico.listarMovimentos(planId);
    }

    @PostMapping("/slots/{slotId}/movimentos/iniciar")
    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @Operation(summary = "Iniciar movimento somente quando a tampa permitir acesso ao slot")
    public ResponseEntity<MovimentoResposta> iniciarMovimento(
            @PathVariable Long planId,
            @PathVariable Long slotId,
            @Valid @RequestBody IniciarMovimentoRequest request,
            Principal principal) {
        MovimentoResposta resposta = servico.iniciarMovimento(
                planId,
                slotId,
                request,
                usuario(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/movimentos-operacionais/{movimentoId}/concluir")
    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @Operation(summary = "Confirmar conclusão de movimento operacional")
    public MovimentoResposta concluirMovimento(
            @PathVariable Long planId,
            @PathVariable Long movimentoId,
            @Valid @RequestBody ConcluirMovimentoRequest request,
            Principal principal) {
        return servico.concluirMovimento(
                planId,
                movimentoId,
                request,
                usuario(principal));
    }

    private String usuario(Principal principal) {
        return principal == null ? "SISTEMA" : principal.getName();
    }
}
