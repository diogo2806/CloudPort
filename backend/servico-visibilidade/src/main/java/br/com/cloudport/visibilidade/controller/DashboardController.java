package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.dto.AlertaDTO;
import br.com.cloudport.visibilidade.dto.DashboardVisibilidadeDTO;
import br.com.cloudport.visibilidade.dto.OcupacaoPatioDTO;
import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.service.VisibilidadeDashboardService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibilidade/dashboard")
public class DashboardController {

    private final VisibilidadeDashboardService dashboardService;

    public DashboardController(VisibilidadeDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public Map<String, Object> getResumo() {
        DashboardVisibilidadeDTO dashboard = dashboardService.obterDashboard();
        Map<String, Object> resumo = new HashMap<>();
        resumo.put("atualizadoEm", dashboard.getAtualizadoEm());
        resumo.put("naviosEmOperacao", dashboard.getNaviosEmOperacao());
        resumo.put("patio", dashboard.getPatio());
        resumo.put("gate", dashboard.getGate());
        resumo.put("alertasAtivos", dashboard.getAlertasAtivos());
        resumo.put("conteineresCriticos", dashboard.getConteineresCriticos());
        resumo.put("totais", Map.of(
                "navios", dashboard.getNaviosEmOperacao() != null ? dashboard.getNaviosEmOperacao().size() : 0,
                "alertas", dashboard.getAlertasAtivos() != null ? dashboard.getAlertasAtivos().size() : 0,
                "conteineresCriticos", dashboard.getConteineresCriticos() != null ? dashboard.getConteineresCriticos().size() : 0
        ));
        return resumo;
    }

    @GetMapping("/navios")
    public List<StatusNavioDTO> getNaviosEmTempoReal() {
        return dashboardService.listarNavios(null);
    }

    @GetMapping("/yard")
    public OcupacaoPatioDTO getCapacidadeYard() {
        return dashboardService.obterOcupacaoPatio();
    }

    @GetMapping("/alertas/criticos")
    public List<AlertaDTO> getAlertasCriticos() {
        return dashboardService.listarAlertas(List.of("alta", "critica"), null, "ativo");
    }
}
