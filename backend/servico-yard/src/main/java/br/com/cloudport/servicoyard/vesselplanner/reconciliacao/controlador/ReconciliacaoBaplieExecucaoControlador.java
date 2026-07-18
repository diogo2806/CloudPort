package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.controlador;

import br.com.cloudport.servicoyard.seguranca.PoliticaAutorizacaoEstiva;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto.ReconciliacaoBaplieExecucaoDTO.ReconciliarRequest;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto.ReconciliacaoBaplieExecucaoDTO.ReconciliacaoResposta;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto.ReconciliacaoBaplieExecucaoDTO.ResolverDivergenciaRequest;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.servico.ReconciliacaoBaplieExecucaoServico;
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
@RequestMapping("/api/vessel-planner/planos/{planoId}/reconciliacoes")
public class ReconciliacaoBaplieExecucaoControlador {

    private final ReconciliacaoBaplieExecucaoServico servico;

    public ReconciliacaoBaplieExecucaoControlador(ReconciliacaoBaplieExecucaoServico servico) {
        this.servico = servico;
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping
    public ResponseEntity<ReconciliacaoResposta> reconciliar(
            @PathVariable Long planoId,
            @RequestBody(required = false) ReconciliarRequest request) {
        String usuario = request == null ? null : request.usuario();
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.reconciliar(planoId, usuario));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.LEITURA)
    @GetMapping("/atual")
    public ResponseEntity<ReconciliacaoResposta> buscarAtual(@PathVariable Long planoId) {
        return ResponseEntity.ok(servico.buscarAtual(planoId));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/{reconciliacaoId}/divergencias/{divergenciaId}/resolver")
    public ResponseEntity<ReconciliacaoResposta> resolverDivergencia(
            @PathVariable Long planoId,
            @PathVariable Long reconciliacaoId,
            @PathVariable Long divergenciaId,
            @Valid @RequestBody ResolverDivergenciaRequest request) {
        return ResponseEntity.ok(servico.resolverDivergencia(
                planoId,
                reconciliacaoId,
                divergenciaId,
                request.decisao(),
                request.motivo(),
                request.usuario()));
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/validar-publicacao")
    public ResponseEntity<Void> validarPublicacao(@PathVariable Long planoId) {
        servico.exigirSemDivergenciasCriticas(planoId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(PoliticaAutorizacaoEstiva.COMANDO)
    @PostMapping("/validar-conclusao")
    public ResponseEntity<Void> validarConclusao(@PathVariable Long planoId) {
        servico.exigirSemDivergenciasCriticas(planoId);
        return ResponseEntity.noContent().build();
    }
}
