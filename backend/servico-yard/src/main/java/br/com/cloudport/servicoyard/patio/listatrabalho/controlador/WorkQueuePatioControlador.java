package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueOrdensDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AuditoriaComandoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico;
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
    private final WorkQueueOperacaoServico workQueueOperacaoServico;
    private final AuditoriaComandoPatioServico auditoriaComandoPatioServico;

    public WorkQueuePatioControlador(WorkQueuePatioServico workQueuePatioServico,
                                      WorkQueueOperacaoServico workQueueOperacaoServico,
                                      AuditoriaComandoPatioServico auditoriaComandoPatioServico) {
        this.workQueuePatioServico = workQueuePatioServico;
        this.workQueueOperacaoServico = workQueueOperacaoServico;
        this.auditoriaComandoPatioServico = auditoriaComandoPatioServico;
    }

    @GetMapping("/work-queues")
    public List<WorkQueuePatioRespostaDto> listar(
            @RequestParam(name = "visitaNavioId", required = false) Long visitaNavioId) {
        return workQueuePatioServico.listar(visitaNavioId);
    }

    @PostMapping("/work-queues")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkQueuePatioRespostaDto criar(@Valid @RequestBody WorkQueuePatioRequisicaoDto dto) {
        return workQueuePatioServico.criar(dto);
    }

    @PatchMapping("/work-queues/{id}/ativar")
    public WorkQueuePatioRespostaDto ativar(@PathVariable Long id,
                                             @Valid @RequestBody ComandoMotivadoDto comando) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.ativar(id);
        auditoriaComandoPatioServico.registrar(id, null, "WORK_QUEUE_ATIVADA_COM_MOTIVO", comando,
                "Work queue ativada.");
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/desativar")
    public WorkQueuePatioRespostaDto desativar(@PathVariable Long id,
                                                @Valid @RequestBody ComandoMotivadoDto comando) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.desativar(id);
        auditoriaComandoPatioServico.registrar(id, null, "WORK_QUEUE_DESATIVADA_COM_MOTIVO", comando,
                "Work queue desativada.");
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/pow")
    public WorkQueuePatioRespostaDto atualizarPow(@PathVariable Long id,
                                                   @Valid @RequestBody AtualizacaoWorkQueuePowDto dto) {
        return workQueueOperacaoServico.atualizarPow(id, dto);
    }

    @PatchMapping("/work-queues/{id}/equipamento")
    public WorkQueuePatioRespostaDto atualizarEquipamento(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoWorkQueueEquipamentoDto dto) {
        return workQueueOperacaoServico.atualizarEquipamento(id, dto);
    }

    @PatchMapping("/work-queues/{id}/ordens")
    public WorkQueuePatioRespostaDto atualizarOrdens(@PathVariable Long id,
                                                      @Valid @RequestBody AtualizacaoWorkQueueOrdensDto dto) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.atualizarOrdens(id, dto.getOrdemIds());
        auditoriaComandoPatioServico.registrar(id, null, "JOB_LIST_ATUALIZADA_COM_MOTIVO", dto,
                "Lista de work instructions atualizada.");
        return resposta;
    }

    @GetMapping("/work-queues/{id}/job-list")
    public List<OrdemTrabalhoPatioRespostaDto> listarJobList(@PathVariable Long id) {
        return workQueuePatioServico.listarJobList(id);
    }
}
