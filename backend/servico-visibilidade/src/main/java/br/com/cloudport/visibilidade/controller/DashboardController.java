package br.com.cloudport.visibilidade.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/visibilidade/dashboard")
public class DashboardController {

    @GetMapping("/resumo")
    public Map<String, Object> getResumo() {
        Map<String, Object> resumo = new HashMap<>();
        resumo.put("mensagem", "Dashboard de Visibilidade em Tempo Real - MVP");
        resumo.put("status", "Em desenvolvimento");
        resumo.put("modulos", new String[]{"Navios", "Yard", "Gate", "Alertas"});
        return resumo;
    }

    // TODO: Implementar endpoints agregados do dashboard
    // - GET /navios (status em tempo real)
    // - GET /yard (capacidade)
    // - GET /alertas/criticos
}