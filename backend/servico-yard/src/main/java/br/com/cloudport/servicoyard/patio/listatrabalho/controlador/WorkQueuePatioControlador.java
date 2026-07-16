package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueOrdensDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
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
    private final WorkQueueOperacaoServico operacaoServico;
    private final AuditoriaComandoPatioServico auditoriaComandoPatioServico;

    public WorkQueuePatioControlador(
            WorkQueuePatioServico workQueuePatioServico,
            WorkQueueOperacaoServico operacaoServico,
            AuditoriaComandoPatioServico auditoriaComandoPatioServico
    ) {
        this.workQueuePatioServico = workQueuePatioServico;
        this.operacaoServico = operacaoServico;
        this.auditoriaComandoPatioServico = auditoriaComandoPatioServico;
    }

    @GetMapping("/work-queues")
    public List<WorkQueuePatioRespostaDto> listar(
            @RequestParam(name = "visitaNavioId", required = false) Long visitaNavioId
    ) {
        return workQueuePatioServico.listar(visitaNavioId);
    }

    @PostMapping("/work-queues")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkQueuePatioRespostaDto criar(@Valid @RequestBody WorkQueuePatioRequisicaoDto dto) {
        return workQueuePatioServico.criar(dto);
    }

    @PatchMapping("/work-queues/{id}/ativar")
    public WorkQueuePatioRespostaDto ativar(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivadoDto comando
    ) {
        return operacaoServico.alterarStatus(id, StatusWorkQueuePatio.ATIVA, comando);
    }

    @PatchMapping("/work-queues/{id}/desativar")
    public WorkQueuePatioRespostaDto desativar(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivadoDto comando
    ) {
        return operacaoServico.alterarStatus(id, StatusWorkQueuePatio.INATIVA, comando);
    }

    @PatchMapping("/work-queues/{id}/pow")
    public WorkQueuePatioRespostaDto atualizarPow(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoWorkQueuePowDto dto
    ) {
        return operacaoServico.atualizarCoberturaLegada(id, dto);
    }

    @PatchMapping("/work-queues/{id}/equipamento")
    public WorkQueuePatioRespostaDto atualizarEquipamento(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoWorkQueueEquipamentoDto dto
    ) {
        return operacaoServico.atualizarEquipamentoLegado(id, dto);
    }

    @PatchMapping("/work-queues/{id}/ordens")
    public WorkQueuePatioRespostaDto atualizarOrdens(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoWorkQueueOrdensDto dto
    ) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.atualizarOrdens(id, dto.getOrdemIds());
        auditoriaComandoPatioServico.registrar(
                id,
                null,
                "JOB_LIST_ATUALIZADA_COM_MOTIVO",
                dto,
                "Lista de work instructions atualizada.");
        return resposta;
    }

    @GetMapping("/work-queues/{id}/job-list")
    public List<OrdemTrabalhoPatioRespostaDto> listarJobList(@PathVariable Long id) {
        return workQueuePatioServico.listarJobList(id);
    }

    @PostMapping("/work-queues/{id}/dispatch")
    public ResultadoDispatchWorkQueueDto despachar(
            @PathVariable Long id,
            @RequestBody(required = false) DispatchWorkQueueDto dto
    ) {
        return operacaoServico.despachar(id, dto);
    }

    @PostMapping("/work-instructions/{id}/reset")
    public OrdemTrabalhoPatioRespostaDto resetarInstrucao(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivadoDto comando
    ) {
        return operacaoServico.resetar(id, converter(comando));
    }

    @PostMapping("/work-instructions/{id}/cancelar")
    public OrdemTrabalhoPatioRespostaDto cancelarInstrucao(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivadoDto comando
    ) {
        return operacaoServico.cancelar(id, converter(comando));
    }

    private ComandoWorkInstructionDto converter(ComandoMotivadoDto origem) {
        ComandoWorkInstructionDto destino = new ComandoWorkInstructionDto();
        destino.setMotivo(origem.getMotivo());
        destino.setUsuario(origem.getUsuario());
        destino.setOrigemAcao(origem.getOrigemAcao());
        destino.setCorrelationId(origem.getCorrelationId());
        return destino;
    }
}
