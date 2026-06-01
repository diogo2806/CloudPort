package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.service.AlertasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/visibilidade/alertas")
public class AlertasController {

    @Autowired
    private AlertasService alertasService;

    @GetMapping
    public List<Alerta> listarAtivos() {
        return alertasService.listarAlertasAtivos();
    }

    @GetMapping("/filtrados")
    public Page<Alerta> buscarFiltrados(
            @RequestParam(required = false) List<String> severidade,
            @RequestParam(required = false) List<String> tipo,
            @RequestParam(defaultValue = "ativo") String status,
            Pageable pageable) {
        return alertasService.buscarAlertasFiltrados(severidade, tipo, status, pageable);
    }

    @PostMapping("/{id}/resolver")
    public void resolver(@PathVariable Long id) {
        alertasService.resolverAlerta(id);
    }
}