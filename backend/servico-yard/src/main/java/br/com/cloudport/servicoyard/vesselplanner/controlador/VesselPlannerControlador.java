package br.com.cloudport.servicoyard.vesselplanner.controlador;

import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRespostaDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.CriarEstivagemPlanRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.RestowAnaliseDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.servico.VesselPlannerServico;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public VesselPlannerControlador(VesselPlannerServico servico) {
        this.servico = servico;
    }

    @PostMapping("/planos")
    public ResponseEntity<EstivagemPlanDto> criarPlano(
            @Valid @RequestBody CriarEstivagemPlanRequisicaoDto requisicao) {
        EstivagemPlanDto dto = servico.criarPlanoDeBayPlan(
                requisicao.getBayPlanId(),
                requisicao.getVisitaNavioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/planos/{id}")
    public ResponseEntity<EstivagemPlanDto> buscarPlano(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PostMapping("/planos/{id}/slots/alocar")
    public ResponseEntity<AlocacaoSlotRespostaDto> alocarContainer(
            @PathVariable Long id,
            @RequestBody AlocacaoSlotRequisicaoDto req) {
        AlocacaoSlotRespostaDto resp = servico.alocarContainer(id, req);
        if (!resp.isSucesso()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/planos/{id}/auto-estivagem")
    public ResponseEntity<EstivagemPlanDto> autoEstivar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.autoEstivar(id));
    }

    @GetMapping("/planos/{id}/estabilidade")
    public ResponseEntity<EstabilidadeDto> estabilidade(@PathVariable Long id) {
        return ResponseEntity.ok(servico.calcularEstabilidade(id));
    }

    @GetMapping("/planos/{id}/restow")
    public ResponseEntity<RestowAnaliseDto> restow(@PathVariable Long id) {
        return ResponseEntity.ok(servico.analisarRestow(id));
    }

    @GetMapping("/planos/{id}/sequenciamento-guindastes")
    public ResponseEntity<SequenciamentoGuindasteDto> sequenciamentoGuindastes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "2") int numGuindastes) {
        return ResponseEntity.ok(servico.sequenciarGuindastes(id, numGuindastes));
    }

    @PostMapping("/planos/{id}/validar")
    public ResponseEntity<EstivagemPlanDto> validarEAprovar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.validarEAprovar(id));
    }
}
