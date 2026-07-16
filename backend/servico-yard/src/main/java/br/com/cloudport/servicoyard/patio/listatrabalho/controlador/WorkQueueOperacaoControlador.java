package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadesWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueRecursosDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.JobListEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkInstructionDrillDownDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','SERVICE_NAVIO')")
public class WorkQueueOperacaoControlador {

    private final WorkQueueOperacaoServico servico;

    public WorkQueueOperacaoControlador(WorkQueueOperacaoServico servico) {
        this.servico = servico;
    }

    @PatchMapping("/work-queues/{id}/recursos-operacionais")
    public WorkQueuePatioRespostaDto associarRecursos(@PathVariable Long id,
                                                       @Valid @RequestBody AtualizacaoWorkQueueRecursosDto dto) {
        return servico.associarRecursos(id, dto);
    }

    @PostMapping("/work-instructions/{id}/suspender")
    public OrdemTrabalhoPatioRespostaDto suspender(@PathVariable Long id,
                                                    @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return servico.suspender(id, dto);
    }

    @PostMapping("/work-instructions/{id}/retomar")
    public OrdemTrabalhoPatioRespostaDto retomar(@PathVariable Long id,
                                                  @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return servico.retomar(id, dto);
    }

    @PostMapping("/work-instructions/{id}/bloquear")
    public OrdemTrabalhoPatioRespostaDto bloquear(@PathVariable Long id,
                                                   @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return servico.bloquear(id, dto);
    }

    @PostMapping("/work-instructions/{id}/concluir")
    public OrdemTrabalhoPatioRespostaDto concluir(@PathVariable Long id,
                                                   @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return servico.concluir(id, dto);
    }

    @PatchMapping("/work-instructions/{id}/prioridades")
    public OrdemTrabalhoPatioRespostaDto atualizarPrioridades(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoPrioridadesWorkInstructionDto dto) {
        return servico.atualizarPrioridades(id, dto);
    }

    @GetMapping("/work-instructions/{id}/drill-down")
    public WorkInstructionDrillDownDto drillDown(@PathVariable Long id) {
        return servico.drillDown(id);
    }

    @GetMapping("/work-instructions/matriz-estados")
    public Map<String, List<String>> matrizEstados() {
        return servico.matrizOficialEstados();
    }

    @GetMapping("/equipamentos/job-lists")
    public List<JobListEquipamentoDto> listarJobListsPorEquipamento(
            @RequestParam(name = "visitaNavioId", required = false) Long visitaNavioId) {
        return servico.listarJobListsPorEquipamento(visitaNavioId);
    }

    @GetMapping("/equipamentos/{id}/job-list")
    public JobListEquipamentoDto obterJobListEquipamento(
            @PathVariable Long id,
            @RequestParam(name = "visitaNavioId", required = false) Long visitaNavioId) {
        return servico.obterJobListEquipamento(id, visitaNavioId);
    }
}
