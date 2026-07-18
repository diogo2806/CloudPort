package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ReplanejarAllocationPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AllocationGraficaPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.EventoOperacaoPatioPublicador;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/work-instructions")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class AllocationGraficaPatioControlador {

    private final AllocationGraficaPatioServico servico;
    private final EventoOperacaoPatioPublicador eventoPublicador;

    public AllocationGraficaPatioControlador(AllocationGraficaPatioServico servico,
                                               EventoOperacaoPatioPublicador eventoPublicador) {
        this.servico = servico;
        this.eventoPublicador = eventoPublicador;
    }

    @PatchMapping("/{id}/allocation")
    public OrdemTrabalhoPatioRespostaDto replanejar(@PathVariable Long id,
                                                      @Valid @RequestBody ReplanejarAllocationPatioDto dto) {
        OrdemTrabalhoPatioRespostaDto resposta = servico.replanejar(id, dto);
        eventoPublicador.publicarInstrucao(resposta, "WORK_INSTRUCTION_ALLOCATION_REPLANEJADA", dto.getCorrelationId());
        return resposta;
    }
}