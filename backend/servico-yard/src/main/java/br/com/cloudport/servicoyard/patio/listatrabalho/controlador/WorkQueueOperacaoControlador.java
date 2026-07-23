package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadesWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueRecursosDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.JobListEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkInstructionDrillDownDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.EventoOperacaoPatioPublicador;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico;
import br.com.cloudport.servicoyard.patio.purgatorio.servico.PurgatorioWorkInstructionServico;
import br.com.cloudport.servicoyard.scheduler.servico.ValidacaoPlanejamentoDispatchServico;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/yard/patio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','OPERADOR_GATE','SERVICE_NAVIO')")
public class WorkQueueOperacaoControlador {

    private static final String AUTORIZACAO_ADMINISTRACAO_PATIO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR')";
    private static final String AUTORIZACAO_OPERACAO_PATIO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')";

    private final WorkQueueOperacaoServico servico;
    private final EventoOperacaoPatioPublicador eventoPublicador;
    private final ValidacaoPlanejamentoDispatchServico validacaoPlanejamentoDispatch;
    private final PurgatorioWorkInstructionServico purgatorioServico;

    public WorkQueueOperacaoControlador(
            WorkQueueOperacaoServico servico,
            EventoOperacaoPatioPublicador eventoPublicador,
            ValidacaoPlanejamentoDispatchServico validacaoPlanejamentoDispatch,
            PurgatorioWorkInstructionServico purgatorioServico) {
        this.servico = servico;
        this.eventoPublicador = eventoPublicador;
        this.validacaoPlanejamentoDispatch = validacaoPlanejamentoDispatch;
        this.purgatorioServico = purgatorioServico;
    }

    @PatchMapping("/work-queues/{id}/recursos-operacionais")
    @PreAuthorize(AUTORIZACAO_ADMINISTRACAO_PATIO)
    public WorkQueuePatioRespostaDto associarRecursos(@PathVariable Long id,
                                                          @Valid @RequestBody AtualizacaoWorkQueueRecursosDto dto) {
        WorkQueuePatioRespostaDto resposta = servico.associarRecursos(id, dto);
        eventoPublicador.publicarWorkQueue(resposta, "WORK_QUEUE_RECURSOS_ASSOCIADOS", dto.getCorrelationId());
        return resposta;
    }

    @PostMapping("/work-queues/{id}/dispatch")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    @Transactional
    public ResultadoDispatchWorkQueueDto despachar(@PathVariable Long id,
                                                       @Valid @RequestBody DispatchWorkQueueDto dto) {
        purgatorioServico.validarDispatch(id);
        validacaoPlanejamentoDispatch.revalidar(id, dto);
        ResultadoDispatchWorkQueueDto resposta = servico.despachar(id, dto);
        eventoPublicador.publicarDispatch(resposta, dto.getCorrelationId());
        return resposta;
    }

    @PostMapping("/work-instructions/{id}/suspender")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto suspender(@PathVariable Long id,
                                                       @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return publicar(servico.suspender(id, dto), "WORK_INSTRUCTION_SUSPENSA", dto.getCorrelationId());
    }

    @PostMapping("/work-instructions/{id}/retomar")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto retomar(@PathVariable Long id,
                                                     @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return publicar(servico.retomar(id, dto), "WORK_INSTRUCTION_RETOMADA", dto.getCorrelationId());
    }

    @PostMapping("/work-instructions/{id}/bloquear")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto bloquear(@PathVariable Long id,
                                                      @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return publicar(servico.bloquear(id, dto), "WORK_INSTRUCTION_BLOQUEADA", dto.getCorrelationId());
    }

    @PostMapping("/work-instructions/{id}/concluir")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto concluir(@PathVariable Long id,
                                                      @Valid @RequestBody ComandoWorkInstructionDto dto) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A conclusao da work instruction exige um evento VMT CONCLUSAO idempotente e persistido.");
    }

    @PostMapping("/work-instructions/{id}/reset")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto resetar(@PathVariable Long id,
                                                     @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return publicar(servico.resetar(id, dto), "WORK_INSTRUCTION_RESETADA", dto.getCorrelationId());
    }

    @PostMapping("/work-instructions/{id}/cancelar")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto cancelar(@PathVariable Long id,
                                                      @Valid @RequestBody ComandoWorkInstructionDto dto) {
        return publicar(servico.cancelar(id, dto), "WORK_INSTRUCTION_CANCELADA", dto.getCorrelationId());
    }

    @PatchMapping("/work-instructions/{id}/prioridades")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public OrdemTrabalhoPatioRespostaDto atualizarPrioridades(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoPrioridadesWorkInstructionDto dto) {
        return publicar(servico.atualizarPrioridades(id, dto),
                "WORK_INSTRUCTION_PRIORIDADE_ALTERADA", dto.getCorrelationId());
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

    private OrdemTrabalhoPatioRespostaDto publicar(OrdemTrabalhoPatioRespostaDto resposta,
                                                       String tipoAlteracao,
                                                       String correlationId) {
        eventoPublicador.publicarInstrucao(resposta, tipoAlteracao, correlationId);
        return resposta;
    }
}
