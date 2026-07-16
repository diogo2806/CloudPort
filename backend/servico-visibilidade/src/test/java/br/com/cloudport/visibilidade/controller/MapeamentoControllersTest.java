package br.com.cloudport.visibilidade.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.RastreamentoConteinerService;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import br.com.cloudport.visibilidade.service.VisibilidadeDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MapeamentoControllersTest {

    @Test
    void deveInicializarTodosOsControllersSemRotasAmbiguas() {
        VisibilidadeDashboardService dashboardService = mock(VisibilidadeDashboardService.class);
        RastreamentoConteinerService rastreamentoService = mock(RastreamentoConteinerService.class);
        AlertasService alertasService = mock(AlertasService.class);
        StatusNavioService statusNavioService = mock(StatusNavioService.class);
        CapacidadeYardService capacidadeYardService = mock(CapacidadeYardService.class);

        assertDoesNotThrow(() -> MockMvcBuilders.standaloneSetup(
                new VisibilidadeController(dashboardService, rastreamentoService),
                new DashboardController(dashboardService),
                new AlertasController(alertasService),
                new NavioController(statusNavioService),
                new YardController(capacidadeYardService))
                .build());
    }
}
