package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.contracts.api.PaginaResposta;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioResumoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.WorkQueuePatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.WorkQueuePatioResumoDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.ConsultaPublicaVisitaNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.ConversorWorkQueuePatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.IntegracaoNavioPatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.PlanoEstivaNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.SeletorCamposPublicos;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/v1")
public class PublicVesselVisitApiControlador {

    private static final int TAMANHO_MAXIMO = 200;
    private static final Set<String> CAMPOS_RESUMO = Set.of(
            "id", "navioId", "navioNome", "codigoVisita", "viagemEntrada", "viagemSaida",
            "linhaOperadora", "bercoPrevisto", "bercoAtual", "eta", "etb", "etd", "fase", "atualizadoEm"
    );
    private static final Set<String> CAMPOS_DETALHE = Set.of(
            "id", "navioId", "navioNome", "codigoVisita", "viagemEntrada", "viagemSaida", "linhaOperadora",
            "terminalFacility", "bercoPrevisto", "bercoAtual", "eta", "ata", "etb", "atb", "inicioOperacao",
            "fimOperacao", "etd", "atd", "janelaRecebimentoInicio", "janelaRecebimentoFim", "cutoffOperacional",
            "fase", "observacoes", "criadoEm", "atualizadoEm"
    );

    private final VisitaNavioServico visitaNavioServico;
    private final ConsultaPublicaVisitaNavioServico consultaPublicaServico;
    private final PlanoEstivaNavioServico planoEstivaNavioServico;
    private final IntegracaoNavioPatioServico integracaoNavioPatioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;
    private final ConversorWorkQueuePatioServico conversorWorkQueue;
    private final SeletorCamposPublicos seletorCampos;

    public PublicVesselVisitApiControlador(VisitaNavioServico visitaNavioServico,
                                             ConsultaPublicaVisitaNavioServico consultaPublicaServico,
                                             PlanoEstivaNavioServico planoEstivaNavioServico,
                                             IntegracaoNavioPatioServico integracaoNavioPatioServico,
                                             OrdemPatioYardCliente ordemPatioYardCliente,
                                             ConversorWorkQueuePatioServico conversorWorkQueue,
                                             SeletorCamposPublicos seletorCampos) {
        this.visitaNavioServico = visitaNavioServico;
        this.consultaPublicaServico = consultaPublicaServico;
        this.planoEstivaNavioServico = planoEstivaNavioServico;
        this.integracaoNavioPatioServico = integracaoNavioPatioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
        this.conversorWorkQueue = conversorWorkQueue;
        this.seletorCampos = seletorCampos;
    }

    @GetMapping("/vessel-visits")
    public PaginaResposta<Map<String, Object>> listarVisitas(
            @RequestParam(required = false) FaseVisitaNavio fase,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Long navioId,
            @RequestParam(required = false) String codigoVisita,
            @RequestParam(required = false) String berco,
            @RequestParam(required = false) String linhaOperadora,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho,
            @RequestParam(defaultValue = "eta") String ordenarPor,
            @RequestParam(defaultValue = "DESC") Sort.Direction direcao,
            @RequestParam(required = false) String campos
    ) {
        PaginaResposta<VisitaNavioResumoDTO> resultado = consultaPublicaServico.listar(
                fase, dataInicio, dataFim, navioId, codigoVisita, berco, linhaOperadora,
                pagina, tamanho, ordenarPor, direcao
        );
        List<Map<String, Object>> conteudo = resultado.conteudo().stream()
                .map(visita -> seletorCampos.selecionar(visita, campos, CAMPOS_RESUMO))
                .toList();
        return copiarPagina(resultado, conteudo);
    }

    @GetMapping("/vessel-visits/{id}")
    public Map<String, Object> detalharVisita(@PathVariable Long id,
                                               @RequestParam(required = false) String campos) {
        VisitaNavioDTO visita = visitaNavioServico.detalhar(id);
        return seletorCampos.selecionar(visita, campos, CAMPOS_DETALHE);
    }

    @GetMapping("/vessel-visits/{id}/stow-plan")
    public PlanoEstivaNavioDTO obterPlanoEstiva(@PathVariable Long id) {
        return planoEstivaNavioServico.obter(id);
    }

    @GetMapping("/vessel-visits/{id}/yard-orders")
    public PaginaResposta<OrdemPatioDaVisitaDTO> listarOrdensPatio(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        Predicate<OrdemPatioDaVisitaDTO> filtro = ordem -> status == null
                || status.isBlank()
                || status.equalsIgnoreCase(ordem.statusOrdem());
        return paginar(integracaoNavioPatioServico.listarOrdensDaVisita(id).stream().filter(filtro).toList(), pagina, tamanho);
    }

    @GetMapping("/vessel-visits/{id}/work-queues")
    public PaginaResposta<WorkQueuePatioResumoDTO> listarWorkQueues(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        List<WorkQueuePatioResumoDTO> filas = consultarWorkQueues(id).stream()
                .map(WorkQueuePatioResumoDTO::de)
                .toList();
        return paginar(filas, pagina, tamanho);
    }

    @GetMapping("/vessel-visits/{id}/work-queues/{workQueueId}")
    public WorkQueuePatioDaVisitaDTO detalharWorkQueue(@PathVariable Long id,
                                                        @PathVariable Long workQueueId) {
        return consultarWorkQueues(id).stream()
                .filter(fila -> workQueueId.equals(fila.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Work queue nao encontrada para a visita informada."
                ));
    }

    @GetMapping("/vessel-visits/{id}/events")
    public PaginaResposta<EventoVisitaNavioDTO> listarEventos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return paginar(visitaNavioServico.eventos(id), pagina, tamanho);
    }

    @GetMapping("/yard/orders")
    public PaginaResposta<OrdemPatioDaVisitaDTO> listarOrdensPublicas(
            @RequestParam Long visitaNavioId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        List<OrdemPatioDaVisitaDTO> ordens = integracaoNavioPatioServico.listarOrdensDaVisita(visitaNavioId).stream()
                .filter(ordem -> status == null || status.isBlank() || status.equalsIgnoreCase(ordem.statusOrdem()))
                .toList();
        return paginar(ordens, pagina, tamanho);
    }

    @GetMapping("/yard/reservations")
    public PaginaResposta<ReservaPatioNavioDTO> listarReservasPublicas(
            @RequestParam Long visitaNavioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return paginar(integracaoNavioPatioServico.listarReservasDaVisita(visitaNavioId), pagina, tamanho);
    }

    private List<WorkQueuePatioDaVisitaDTO> consultarWorkQueues(Long visitaId) {
        visitaNavioServico.buscarEntidade(visitaId);
        try {
            return ordemPatioYardCliente.listarWorkQueuesDaVisita(visitaId).stream()
                    .map(conversorWorkQueue::converter)
                    .toList();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel consultar as work queues no servico-yard.",
                    ex
            );
        }
    }

    private <T> PaginaResposta<T> paginar(List<T> valores, int pagina, int tamanho) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);
        int inicio = Math.min(paginaSegura * tamanhoSeguro, valores.size());
        int fim = Math.min(inicio + tamanhoSeguro, valores.size());
        int totalPaginas = valores.isEmpty() ? 0 : (int) Math.ceil((double) valores.size() / tamanhoSeguro);
        return new PaginaResposta<>(
                valores.subList(inicio, fim),
                paginaSegura,
                tamanhoSeguro,
                valores.size(),
                totalPaginas,
                paginaSegura == 0,
                totalPaginas == 0 || paginaSegura >= totalPaginas - 1
        );
    }

    private <T, R> PaginaResposta<R> copiarPagina(PaginaResposta<T> origem, List<R> conteudo) {
        return new PaginaResposta<>(
                conteudo,
                origem.pagina(),
                origem.tamanho(),
                origem.totalElementos(),
                origem.totalPaginas(),
                origem.primeira(),
                origem.ultima()
        );
    }
}
