package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.WorkQueuePatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.WorkQueuePatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.IntegracaoNavioPatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.PlanoEstivaNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1")
public class PublicVesselVisitApiControlador {

    private final VisitaNavioServico visitaNavioServico;
    private final PlanoEstivaNavioServico planoEstivaNavioServico;
    private final IntegracaoNavioPatioServico integracaoNavioPatioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;

    public PublicVesselVisitApiControlador(VisitaNavioServico visitaNavioServico,
                                           PlanoEstivaNavioServico planoEstivaNavioServico,
                                           IntegracaoNavioPatioServico integracaoNavioPatioServico,
                                           OrdemPatioYardCliente ordemPatioYardCliente) {
        this.visitaNavioServico = visitaNavioServico;
        this.planoEstivaNavioServico = planoEstivaNavioServico;
        this.integracaoNavioPatioServico = integracaoNavioPatioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
    }

    @GetMapping("/vessel-visits")
    public List<VisitaNavioDTO> listarVisitas(
            @RequestParam(required = false) FaseVisitaNavio fase,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Long navioId
    ) {
        return visitaNavioServico.listar(fase, dataInicio, dataFim, navioId);
    }

    @GetMapping("/vessel-visits/{id}")
    public VisitaNavioDTO detalharVisita(@PathVariable Long id) {
        return visitaNavioServico.detalhar(id);
    }

    @GetMapping("/vessel-visits/{id}/stow-plan")
    public PlanoEstivaNavioDTO obterPlanoEstiva(@PathVariable Long id) {
        return planoEstivaNavioServico.obter(id);
    }

    @GetMapping("/vessel-visits/{id}/yard-orders")
    public List<OrdemPatioDaVisitaDTO> listarOrdensPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.listarOrdensDaVisita(id);
    }

    @GetMapping("/vessel-visits/{id}/work-queues")
    public List<WorkQueuePatioDaVisitaDTO> listarWorkQueues(@PathVariable Long id) {
        visitaNavioServico.buscarEntidade(id);
        try {
            return ordemPatioYardCliente.listarWorkQueuesDaVisita(id).stream()
                    .map(this::converterWorkQueue)
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    @GetMapping("/vessel-visits/{id}/events")
    public List<EventoVisitaNavioDTO> listarEventos(@PathVariable Long id) {
        return visitaNavioServico.eventos(id);
    }

    @GetMapping("/yard/orders")
    public List<OrdemPatioDaVisitaDTO> listarOrdensPublicas(@RequestParam Long visitaNavioId,
                                                             @RequestParam(required = false) String status) {
        return integracaoNavioPatioServico.listarOrdensDaVisita(visitaNavioId).stream()
                .filter(ordem -> !StringUtils.hasText(status) || status.equalsIgnoreCase(ordem.statusOrdem()))
                .toList();
    }

    @GetMapping("/yard/reservations")
    public List<ReservaPatioNavioDTO> listarReservasPublicas(@RequestParam Long visitaNavioId) {
        return integracaoNavioPatioServico.listarReservasDaVisita(visitaNavioId);
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
        return new OrdemPatioDaVisitaDTO(
                ordem.getId(),
                ordem.getVisitaNavioId(),
                ordem.getItemOperacaoNavioId(),
                ordem.getCodigoConteiner(),
                tipoMovimento,
                ordem.getStatusOrdem(),
                tipoMovimento == TipoMovimentoNavio.DESCARGA ? "NAVIO" : ordem.getDestino(),
                tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "NAVIO" : ordem.getDestino(),
                ordem.posicaoDestinoFormatada(),
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
