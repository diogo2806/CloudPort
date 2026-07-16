package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.service.AlertasService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibilidade/alertas")
public class AlertasController {

    private final AlertasService alertasService;

    public AlertasController(AlertasService alertasService) {
        this.alertasService = alertasService;
    }

    @GetMapping("/filtrados")
    public Page<Alerta> buscarFiltrados(@RequestParam(required = false) List<String> severidade,
                                        @RequestParam(required = false) List<String> tipo,
                                        @RequestParam(defaultValue = "ativo") String status,
                                        Pageable pageable) {
        return alertasService.buscarAlertasFiltrados(severidade, tipo, status, pageable);
    }
}
