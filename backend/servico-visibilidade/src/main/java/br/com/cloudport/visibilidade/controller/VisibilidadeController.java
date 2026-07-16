package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.dto.AlertaDTO;
import br.com.cloudport.visibilidade.dto.ConteinerBuscaDTO;
import br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO;
import br.com.cloudport.visibilidade.dto.DashboardVisibilidadeDTO;
import br.com.cloudport.visibilidade.dto.NavioDetalhadoDTO;
import br.com.cloudport.visibilidade.dto.OcupacaoPatioDTO;
import br.com.cloudport.visibilidade.dto.ResolverAlertaRequestDTO;
import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.dto.ThroughputGateDTO;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.service.RastreamentoConteinerService;
import br.com.cloudport.visibilidade.service.VisibilidadeDashboardService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibilidade")
public class VisibilidadeController {

    private final VisibilidadeDashboardService service;
    private final RastreamentoConteinerService rastreamentoConteinerService;

    public VisibilidadeController(VisibilidadeDashboardService service,
                                  RastreamentoConteinerService rastreamentoConteinerService) {
        this.service = service;
        this.rastreamentoConteinerService = rastreamentoConteinerService;
    }

    @GetMapping("/dashboard")
    public DashboardVisibilidadeDTO dashboard() {
        return service.obterDashboard();
    }

    @GetMapping("/navios")
    public List<StatusNavioDTO> navios(@RequestParam(required = false) List<String> status) {
        return service.listarNavios(status);
    }

    @GetMapping("/navios/{navioId}/detalhes")
    public NavioDetalhadoDTO detalhesNavio(@PathVariable String navioId) {
        return service.obterDetalhesNavio(navioId);
    }

    @GetMapping("/patio/ocupacao")
    public OcupacaoPatioDTO ocupacaoPatio() {
        return service.obterOcupacaoPatio();
    }

    @GetMapping("/gate/throughput")
    public ThroughputGateDTO throughputGate() {
        return service.obterThroughputGate();
    }

    @GetMapping("/alertas")
    public List<AlertaDTO> alertas(@RequestParam(required = false) List<String> severidade,
                                   @RequestParam(required = false) List<String> tipo,
                                   @RequestParam(defaultValue = "ativo") String status) {
        return service.listarAlertas(severidade, tipo, status);
    }

    @GetMapping("/alertas/{id}")
    public AlertaDTO alerta(@PathVariable Long id) {
        return service.obterAlerta(id);
    }

    @PostMapping("/alertas/{id}/resolver")
    public AlertaDTO resolverAlerta(@PathVariable Long id,
                                    @RequestBody(required = false) ResolverAlertaRequestDTO request) {
        return service.resolverAlerta(id, request != null ? request.getMotivo() : null);
    }

    @GetMapping("/conteiners/{containerId}/track")
    public ConteinerRastreamentoDTO rastrear(@PathVariable String containerId) {
        return rastreamentoConteinerService.rastrearContainer(containerId);
    }

    @GetMapping("/conteiners/{containerId}/historico")
    public List<HistoricoMovimento> historico(@PathVariable String containerId) {
        return rastreamentoConteinerService.obterHistorico(containerId);
    }

    @GetMapping("/conteiners/buscar")
    public Page<ConteinerBuscaDTO> buscarContainers(@RequestParam(required = false) String containerId,
                                                     @RequestParam(required = false) String statusAtual,
                                                     @RequestParam(required = false) String zonaYard,
                                                     @RequestParam(required = false) String navioDestino,
                                                     Pageable pageable) {
        return service.buscarContainers(containerId, statusAtual, zonaYard, navioDestino, pageable);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErro(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
