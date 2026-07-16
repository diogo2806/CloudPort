package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dto.WorkQueuePatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.ConversorWorkQueuePatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/visitas-navio/{visitaId}/integracao-patio/work-queues")
public class WorkQueuePatioNavioControlador {

    private final VisitaNavioServico visitaNavioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;
    private final ConversorWorkQueuePatioServico conversor;

    public WorkQueuePatioNavioControlador(VisitaNavioServico visitaNavioServico,
                                           OrdemPatioYardCliente ordemPatioYardCliente,
                                           ConversorWorkQueuePatioServico conversor) {
        this.visitaNavioServico = visitaNavioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
        this.conversor = conversor;
    }

    @GetMapping
    public List<WorkQueuePatioDaVisitaDTO> listar(@PathVariable Long visitaId) {
        visitaNavioServico.buscarEntidade(visitaId);
        try {
            return ordemPatioYardCliente.listarWorkQueuesDaVisita(visitaId).stream()
                    .map(conversor::converter)
                    .toList();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel consultar as work queues no servico-yard.",
                    ex
            );
        }
    }
}
