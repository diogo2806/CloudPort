package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueOrdensDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AuditoriaComandoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.EventoOperacaoPatioPublicador;
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
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','OPERADOR_GATE','SERVICE_NAVIO')")
public class WorkQueuePatioControlador {

    private static final String AUTORIZACAO_ADMINISTRACAO_PATIO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR')";

    private final WorkQueuePatioServico workQueuePatioServico;
    private final WorkQueueOperacaoServico workQueueOperacaoServico;
    private final AuditoriaComandoPatioServico auditoriaComandoPatioServico;
    private final EventoOperacaoPatioPublicador eventoPublicador;

    public WorkQueuePatioControlador(WorkQueuePatioServico workQueuePatioServico,
                                       WorkQueueOperacaoServico workQueueOperacaoServico,
                                       AuditoriaComandoPatioServico auditoriaComandoPatioServico,
                                       EventoOperacaoPatioPublicador eventoPublicador) {
        this.workQueuePatioServico = workQueuePatioServico;
        this.workQueueOperacaoServico = workQueueOperacaoServico;
        this.auditoriaComandoPatioServico = auditoriaComandoPatioServico;
        this.eventoPublicador = eventoPublicador;
    }

    @GetMapping("/work-queues")
    public List<WorkQueuePatioRespostaDto> listar(
            @RequestParam(name = "visitaNavioId", required = false) Long visitaNavioId) {
        return workQueuePatioServico.listar(visitaNavioId);
    }

    @PostMapping("/work-queues")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto criar(@Valid @RequestBody WorkQueuePatioRequisicaoDto dto) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.criar(dto);
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_CRIADA", null);
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/ativar")
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto ativar(@PathVariable Long id,
                                              @Valid @RequestBody ComandoMotivadoDto comando) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.ativar(id);
        auditoriaComandoPatioServico.registrar(id, null, "WORK_QUEUE_ATIVADA_COM_MOTIVO", comando,
                "Work queue ativada.");
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_ATIVADA", comando.getCorrelationId());
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/desativar")
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto desativar(@PathVariable Long id,
                                                 @Valid @RequestBody ComandoMotivadoDto comando) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.desativar(id);
        auditoriaComandoPatioServico.registrar(id, null, "WORK_QUEUE_DESATIVADA_COM_MOTIVO", comando,
                "Work queue desativada.");
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_DESATIVADA", comando.getCorrelationId());
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/pow")
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto atualizarPow(@PathVariable Long id,
                                                    @Valid @RequestBody AtualizacaoWorkQueuePowDto dto) {
        WorkQueuePatioRespostaDto resposta = workQueueOperacaoServico.atualizarPow(id, dto);
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_COBERTURA_ATUALIZADA", dto.getCorrelationId());
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/equipamento")
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto atualizarEquipamento(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoWorkQueueEquipamentoDto dto) {
        WorkQueuePatioRespostaDto resposta = workQueueOperacaoServico.atualizarEquipamento(id, dto);
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_EQUIPAMENTO_ATUALIZADO", dto.getCorrelationId());
        return resposta;
    }

    @PatchMapping("/work-queues/{id}/ordens")
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto atualizarOrdens(@PathVariable Long id,
                                                       @Valid @RequestBody AtualizacaoWorkQueueOrdensDto dto) {
        WorkQueuePatioRespostaDto resposta = workQueuePatioServico.atualizarOrdens(id, dto.getOrdemIds());
        auditoriaComandoPatioServico.registrar(id, null, "JOB_LIST_ATUALIZADA_COM_MOTIVO", dto,
                "Lista de work instructions atualizada.");
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_JOB_LIST_ATUALIZADA", dto.getCorrelationId());
        return resposta;
    }

    @GetMapping("/work-queues/{id}/job-list")
    public List<OrdemTrabalhoPatioRespostaDto> listarJobList(@PathVariable Long id) {
        return workQueuePatioServico.listarJobList(id);
    }
}
