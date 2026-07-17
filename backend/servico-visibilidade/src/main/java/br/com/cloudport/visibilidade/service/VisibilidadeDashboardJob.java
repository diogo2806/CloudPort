package br.com.cloudport.visibilidade.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cloudport.runtime",
        name = "jobs-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VisibilidadeDashboardJob {

    private final VisibilidadeDashboardService visibilidadeDashboardService;

    public VisibilidadeDashboardJob(VisibilidadeDashboardService visibilidadeDashboardService) {
        this.visibilidadeDashboardService = visibilidadeDashboardService;
    }

    @Scheduled(fixedDelayString = "${visibilidade.dashboard.refresh-ms:30000}")
    public void publicarDashboard() {
        visibilidadeDashboardService.publicarDashboard();
    }

    @Scheduled(fixedDelayString = "${visibilidade.alertas.refresh-ms:60000}")
    public void detectarAlertasAutomaticos() {
        visibilidadeDashboardService.detectarAlertasAutomaticos();
    }
}
