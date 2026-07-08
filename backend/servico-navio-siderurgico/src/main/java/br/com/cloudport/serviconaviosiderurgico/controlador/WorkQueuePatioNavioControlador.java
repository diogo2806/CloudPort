package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.WorkQueuePatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.WorkQueuePatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visitas-navio/{visitaId}/integracao-patio/work-queues")
public class WorkQueuePatioNavioControlador {

    private final VisitaNavioServico visitaNavioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;

    public WorkQueuePatioNavioControlador(VisitaNavioServico visitaNavioServico,
                                          OrdemPatioYardCliente ordemPatioYardCliente) {
        this.visitaNavioServico = visitaNavioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
    }

    @GetMapping
    public List<WorkQueuePatioDaVisitaDTO> listar(@PathVariable Long visitaId) {
        visitaNavioServico.buscarEntidade(visitaId);
        try {
            return ordemPatioYardCliente.listarWorkQueuesDaVisita(visitaId).stream()
                    .map(this::converterWorkQueue)
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private WorkQueuePatioDaVisitaDTO converterWorkQueue(WorkQueuePatioYardDTO fila) {
        List<OrdemPatioDaVisitaDTO> jobList = Optional.ofNullable(fila.getJobList())
                .orElse(List.of()).stream()
                .map(this::converterOrdem)
                .toList();
        return new WorkQueuePatioDaVisitaDTO(
                fila.getId(),
                fila.getIdentificador(),
                fila.getAgrupamento(),
                fila.getVisitaNavioId(),
                fila.getBerco(),
                fila.getPorao(),
                fila.getBlocoZona(),
                fila.getSequenciaInicial(),
                fila.getPow(),
                fila.getPoolOperacional(),
                fila.getEquipamento(),
                fila.getStatus(),
                fila.getPrioridadeOperacional(),
                fila.getTotalOrdens(),
                jobList,
                fila.getCriadoEm(),
                fila.getAtualizadoEm()
        );
    }

    private OrdemPatioDaVisitaDTO converterOrdem(OrdemPatioYardRespostaDTO ordem) {
        TipoMovimentoNavio tipoMovimento = tipoMovimentoNavio(ordem.getTipoMovimento());
        String destinoFormatado = ordem.posicaoDestinoFormatada();
        return new OrdemPatioDaVisitaDTO(
                ordem.getId(),
                ordem.getVisitaNavioId(),
                ordem.getItemOperacaoNavioId(),
                ordem.getCodigoConteiner(),
                tipoMovimento,
                ordem.getStatusOrdem(),
                tipoMovimento == TipoMovimentoNavio.DESCARGA ? "NAVIO" : ordem.getDestino(),
                tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "NAVIO" : ordem.getDestino(),
                destinoFormatado,
                null,
                ordem.getSequenciaNavio(),
                ordem.getPrioridadeOperacional()
        );
    }

    private TipoMovimentoNavio tipoMovimentoNavio(String tipoMovimentoPatio) {
        if (!StringUtils.hasText(tipoMovimentoPatio)) {
            return TipoMovimentoNavio.DESCARGA;
        }
        return switch (tipoMovimentoPatio.toUpperCase(Locale.ROOT)) {
            case "TRANSFERENCIA" -> TipoMovimentoNavio.EMBARQUE;
            case "REMANEJAMENTO" -> TipoMovimentoNavio.RESTOW;
            default -> TipoMovimentoNavio.DESCARGA;
        };
    }
}
