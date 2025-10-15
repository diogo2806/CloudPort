package br.com.cloudport.servicogate.app.transparencia;

import br.com.cloudport.servicogate.app.transparencia.dto.DashboardFiltroDTO;
import br.com.cloudport.servicogate.app.transparencia.dto.DashboardResumoDTO;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.app.transparencia.DashboardService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/gate/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public DashboardResumoDTO consultar(
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(value = "transportadoraId", required = false) Long transportadoraId,
            @RequestParam(value = "tipoOperacao", required = false) String tipoOperacao
    ) {
        DashboardFiltroDTO filtro = construirFiltro(inicio, fim, transportadoraId, tipoOperacao);
        return dashboardService.obterResumo(filtro);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public SseEmitter streamDashboard() {
        return dashboardService.registrarAssinante();
    }

    private DashboardFiltroDTO construirFiltro(LocalDateTime inicio, LocalDateTime fim,
                                               Long transportadoraId, String tipoOperacao) {
        DashboardFiltroDTO filtro = new DashboardFiltroDTO();
        filtro.setInicio(inicio);
        filtro.setFim(fim);
        filtro.setTransportadoraId(transportadoraId);
        if (tipoOperacao != null && !tipoOperacao.isBlank()) {
            filtro.setTipoOperacao(TipoOperacao.valueOf(tipoOperacao.toUpperCase()));
        }
        return filtro;
    }
}
