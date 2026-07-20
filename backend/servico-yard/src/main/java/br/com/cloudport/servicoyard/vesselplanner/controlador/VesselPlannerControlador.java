package br.com.cloudport.servicoyard.vesselplanner.controlador;

import br.com.cloudport.servicoyard.seguranca.PoliticaAutorizacaoEstiva;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRespostaDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.CriarEstivagemPlanRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.EncerrarParalisacaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.EventoOperacionalGuindasteResponse;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.RegistrarHandoverRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.RegistrarParalisacaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ConcluirMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.CriarExecucaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ExecucaoResponse;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.FalharMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.IniciarMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ReconciliarExecucaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ReplanejarMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ReconciliacaoBaplieExecucaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ResolverDivergenciaRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.RestowAnaliseDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.servico.EventoOperacionalGuindasteServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.ExecucaoSequenciaGuindasteServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.ReconciliacaoBaplieExecucaoServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.VesselPlannerServico;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vessel-planner")
public class VesselPlannerControlador {

    private final VesselPlannerServico servico;
    private final ExecucaoSequenciaGuindasteServico execucaoGuindasteServico;
    private final EventoOperacionalGuindasteServico eventoOperacionalGuindasteServico;
    private final ReconciliacaoBaplieExecucaoServico reconciliacaoServico;

    public VesselPlannerControlador(
            VesselPlannerServico servico,
            ExecucaoSequenciaGuindasteServico execucaoGuindasteServico,
            EventoOperacionalGuindasteServico eventoOperacionalGuindasteServico,
            ReconciliacaoBaplieExecucaoServico reconciliacaoServico) {
        this.servico = servico;
        this.execucaoGuindasteServico = execucaoGuindasteServico;
        this.eventoOperacionalGuindasteServico = eventoOperacionalGuindasteServico;
        this.reconciliacaoServico = reconciliacaoServico;
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos")
    public ResponseEntity<EstivagemPlanDto> criarPlano(
            @Valid @RequestBody CriarEstivagemPlanRequisicaoDto requisicao) {
        EstivagemPlanDto dto = servico.criarPlanoDeBayPlan(
                requisicao.getBayPlanId(),
                requisicao.getVisitaNavioId());
        if (dto.getId() != null) {
            reconciliacaoServico.reconciliar(dto.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/planos/{id}")
    public ResponseEntity<EstivagemPlanDto> buscarPlano(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos/{id}/slots/alocar")
    public ResponseEntity<AlocacaoSlotRespostaDto> alocarContainer(
            @PathVariable Long id,
            @RequestBody AlocacaoSlotRequisicaoDto req) {
        AlocacaoSlotRespostaDto resp = servico.alocarContainer(id, req);
        if (!resp.isSucesso()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }
        reconciliacaoServico.reconciliar(id);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos/{id}/auto-estivagem")
    public ResponseEntity<EstivagemPlanDto> autoEstivar(@PathVariable Long id) {
        EstivagemPlanDto dto = servico.autoEstivar(id);
        reconciliacaoServico.reconciliar(id);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/planos/{id}/estabilidade")
    public ResponseEntity<EstabilidadeDto> estabilidade(@PathVariable Long id) {
        return ResponseEntity.ok(servico.calcularEstabilidade(id));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/planos/{id}/restow")
    public ResponseEntity<RestowAnaliseDto> restow(@PathVariable Long id) {
        return ResponseEntity.ok(servico.analisarRestow(id));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/planos/{id}/sequenciamento-guindastes")
    public ResponseEntity<SequenciamentoGuindasteDto> sequenciamentoGuindastes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "2") int numGuindastes) {
        return ResponseEntity.ok(servico.sequenciarGuindastes(id, numGuindastes));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos/{id}/execucao-guindastes")
    public ResponseEntity<ExecucaoResponse> criarExecucaoGuindastes(
            @PathVariable Long id,
            @Valid @RequestBody CriarExecucaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(execucaoGuindasteServico.criar(id, request));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/planos/{id}/execucao-guindastes")
    public ResponseEntity<ExecucaoResponse> buscarExecucaoGuindastes(@PathVariable Long id) {
        return ResponseEntity.ok(execucaoGuindasteServico.buscarPorPlano(id));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/execucoes-guindastes/{execucaoId}/eventos-operacionais")
    public ResponseEntity<List<EventoOperacionalGuindasteResponse>> listarEventosOperacionais(
            @PathVariable Long execucaoId) {
        return ResponseEntity.ok(eventoOperacionalGuindasteServico.listar(execucaoId));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/paralisacoes")
    public ResponseEntity<EventoOperacionalGuindasteResponse> registrarParalisacaoGuindaste(
            @PathVariable Long execucaoId,
            @Valid @RequestBody RegistrarParalisacaoRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                eventoOperacionalGuindasteServico.registrarParalisacao(
                        execucaoId,
                        request,
                        usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/paralisacoes/{eventoId}/encerrar")
    public ResponseEntity<EventoOperacionalGuindasteResponse> encerrarParalisacaoGuindaste(
            @PathVariable Long execucaoId,
            @PathVariable Long eventoId,
            @Valid @RequestBody EncerrarParalisacaoRequest request,
            Principal principal) {
        return ResponseEntity.ok(eventoOperacionalGuindasteServico.encerrarParalisacao(
                execucaoId,
                eventoId,
                request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/handovers")
    public ResponseEntity<EventoOperacionalGuindasteResponse> registrarHandoverTurno(
            @PathVariable Long execucaoId,
            @Valid @RequestBody RegistrarHandoverRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                eventoOperacionalGuindasteServico.registrarHandover(
                        execucaoId,
                        request,
                        usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/movimentos/{movimentoId}/iniciar")
    public ResponseEntity<ExecucaoResponse> iniciarMovimentoGuindaste(
            @PathVariable Long execucaoId,
            @PathVariable Long movimentoId,
            @Valid @RequestBody IniciarMovimentoRequest request,
            Principal principal) {
        return ResponseEntity.ok(execucaoGuindasteServico.iniciar(
                execucaoId,
                movimentoId,
                request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/movimentos/{movimentoId}/concluir")
    public ResponseEntity<ExecucaoResponse> concluirMovimentoGuindaste(
            @PathVariable Long execucaoId,
            @PathVariable Long movimentoId,
            @Valid @RequestBody ConcluirMovimentoRequest request,
            Principal principal) {
        return ResponseEntity.ok(execucaoGuindasteServico.concluir(
                execucaoId,
                movimentoId,
                request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/movimentos/{movimentoId}/falhar")
    public ResponseEntity<ExecucaoResponse> falharMovimentoGuindaste(
            @PathVariable Long execucaoId,
            @PathVariable Long movimentoId,
            @Valid @RequestBody FalharMovimentoRequest request,
            Principal principal) {
        return ResponseEntity.ok(execucaoGuindasteServico.falhar(
                execucaoId,
                movimentoId,
                request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/movimentos/{movimentoId}/replanejar")
    public ResponseEntity<ExecucaoResponse> replanejarMovimentoGuindaste(
            @PathVariable Long execucaoId,
            @PathVariable Long movimentoId,
            @Valid @RequestBody ReplanejarMovimentoRequest request,
            Principal principal) {
        return ResponseEntity.ok(execucaoGuindasteServico.replanejar(
                execucaoId,
                movimentoId,
                request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/execucoes-guindastes/{execucaoId}/reconciliar")
    public ResponseEntity<ExecucaoResponse> reconciliarExecucaoGuindastes(
            @PathVariable Long execucaoId,
            @Valid @RequestBody ReconciliarExecucaoRequest request,
            Principal principal) {
        return ResponseEntity.ok(execucaoGuindasteServico.reconciliar(
                execucaoId,
                request,
                usuario(principal)));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/planos/{id}/reconciliacao")
    public ResponseEntity<ReconciliacaoBaplieExecucaoDto> consultarReconciliacao(
            @PathVariable Long id) {
        return ResponseEntity.ok(reconciliacaoServico.consultar(id));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos/{id}/reconciliacao")
    public ResponseEntity<ReconciliacaoBaplieExecucaoDto> reconciliar(
            @PathVariable Long id) {
        return ResponseEntity.ok(reconciliacaoServico.reconciliar(id));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos/{id}/reconciliacao/{divergenciaId}/resolver")
    public ResponseEntity<ReconciliacaoBaplieExecucaoDto> resolverDivergencia(
            @PathVariable Long id,
            @PathVariable Long divergenciaId,
            @Valid @RequestBody ResolverDivergenciaRequisicaoDto requisicao) {
        return ResponseEntity.ok(reconciliacaoServico.resolver(
                id,
                divergenciaId,
                requisicao));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/planos/{id}/validar")
    public ResponseEntity<EstivagemPlanDto> validarEAprovar(@PathVariable Long id) {
        reconciliacaoServico.reconciliar(id);
        reconciliacaoServico.exigirSemDivergenciasCriticas(id);
        return ResponseEntity.ok(servico.validarEAprovar(id));
    }

    private String usuario(Principal principal) {
        return principal == null ? "SISTEMA" : principal.getName();
    }
}
