package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueValidacaoPlanoServico;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/work-queues")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','SERVICE_NAVIO')")
public class WorkQueueValidacaoPlanoControlador {

    private final WorkQueueValidacaoPlanoServico servico;

    public WorkQueueValidacaoPlanoControlador(WorkQueueValidacaoPlanoServico servico) {
        this.servico = servico;
    }

    @GetMapping("/validacao-plano")
    public List<WorkQueueValidacaoPlanoDto> consultar(
            @RequestParam(name = "visitaNavioId") Long visitaNavioId
    ) {
        return servico.consultar(visitaNavioId);
    }
}
