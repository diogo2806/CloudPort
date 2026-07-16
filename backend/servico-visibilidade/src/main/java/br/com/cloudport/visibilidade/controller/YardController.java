package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibilidade/yard")
public class YardController {

    private final CapacidadeYardService capacidadeYardService;

    public YardController(CapacidadeYardService capacidadeYardService) {
        this.capacidadeYardService = capacidadeYardService;
    }

    @GetMapping("/capacity")
    public CapacidadeYard getCapacity(@RequestParam String zona) {
        return capacidadeYardService.getCapacidadePorZona(zona);
    }
}
