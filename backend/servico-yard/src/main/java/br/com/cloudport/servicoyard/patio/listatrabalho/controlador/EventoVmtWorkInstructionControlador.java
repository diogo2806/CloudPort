package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRequest;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.EventoOperacaoPatioPublicador;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.EventoVmtWorkInstructionServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/work-instructions/{instructionId}/vmt-events")
@Tag(name = "Work instructions VMT", description = "Confirmações VMT idempotentes das ordens operacionais de pátio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','INTEGRACAO_VMT')")
public class EventoVmtWorkInstructionControlador {

    private final EventoVmtWorkInstructionServico servico;
    private final EventoOperacaoPatioPublicador eventoPublicador;

    public EventoVmtWorkInstructionControlador(EventoVmtWorkInstructionServico servico,
                                                EventoOperacaoPatioPublicador eventoPublicador) {
        this.servico = servico;
        this.eventoPublicador = eventoPublicador;
    }

    @PostMapping
    @Operation(summary = "Confirma aceite, início, falha ou conclusão enviados pelo VMT")
    public EventoVmtWorkInstructionRespostaDto processar(
            @PathVariable Long instructionId,
            @Valid @RequestBody EventoVmtWorkInstructionRequest request) {
        EventoVmtWorkInstructionRespostaDto resposta = servico.processar(instructionId, request);
        eventoPublicador.publicarInstrucao(
                resposta.getInstrucao(),
                "WORK_INSTRUCTION_VMT_" + resposta.getTipoEvento().name(),
                request.getCorrelationId());
        return resposta;
    }

    @GetMapping
    @Operation(summary = "Lista o ciclo persistido de confirmações VMT da work instruction")
    public List<EventoVmtWorkInstructionRespostaDto> listar(@PathVariable Long instructionId) {
        return servico.listar(instructionId);
    }
}
