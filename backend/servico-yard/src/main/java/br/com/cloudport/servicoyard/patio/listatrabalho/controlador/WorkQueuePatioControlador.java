package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueuePatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','SERVICE_NAVIO')")
public class WorkQueuePatioControlador {

    private final WorkQueuePatioServico workQueuePatioServico;

    public WorkQueuePatioControlador(WorkQueuePatioServico workQueuePatioServico) {
        this.workQueuePatioServico = workQueuePatioServico;
    }

    @GetMapping("/work-queues")
    public List<WorkQueuePatioRespostaDto> listar(@RequestParam(name = "visitaNavioId", required = false) Long visitaNavioId) {
        return workQueuePatioServico.listar(visitaNavioId);
    }

    @PostMapping("/work-queues")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkQueuePatioRespostaDto criar(@Valid @RequestBody WorkQueuePatioRequisicaoDto dto) {
        return workQueuePatioServico.criar(dto);
    }

    @PatchMapping("/work-queues/{id}/ativar")
    public WorkQueuePatioRespostaDto ativar(@PathVariable Long id) {
        return workQueuePatioServico.ativar(id);
    }

    @PatchMapping("/work-queues/{id}/desativar")
    public WorkQueuePatioRespostaDto desativar(@PathVariable Long id) {
        return workQueuePatioServico.desativar(id);
    }

    @PatchMapping("/work-queues/{id}/pow")
    public WorkQueuePatioRespostaDto atualizarPow(@PathVariable Long id,
                                                    @RequestBody AtualizacaoWorkQueuePowDto dto) {
        return workQueuePatioServico.atualizarPow(id, dto);
    }

    @PatchMapping("/work-queues/{id}/equipamento")
    public WorkQueuePatioRespostaDto atualizarEquipamento(@PathVariable Long id,
                                                            @RequestBody AtualizacaoWorkQueueEquipamentoDto dto) {
        return workQueuePatioServico.atualizarEquipamento(id, dto);
    }

    @GetMapping("/work-queues/{id}/job-list")
    public List<OrdemTrabalhoPatioRespostaDto> listarJobList(@PathVariable Long id) {
        return workQueuePatioServico.listarJobList(id);
    }

    @PostMapping("/work-queues/{id}/dispatch")
    public ResultadoDispatchWorkQueueDto despachar(@PathVariable Long id,
                                                     @RequestBody(required = false) DispatchWorkQueueDto dto) {
        return workQueuePatioServico.despachar(id, dto);
    }

    @PostMapping("/work-instructions/{id}/reset")
    public OrdemTrabalhoPatioRespostaDto resetarInstrucao(@PathVariable Long id) {
        return workQueuePatioServico.resetarInstrucao(id);
    }

    @PostMapping("/work-instructions/{id}/cancelar")
    public OrdemTrabalhoPatioRespostaDto cancelarInstrucao(@PathVariable Long id) {
        return workQueuePatioServico.cancelarInstrucao(id);
    }
}
