package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visibilidade/yard")
public class YardController {

    @Autowired
    private CapacidadeYardService capacidadeYardService;

    @GetMapping("/capacity")
    public CapacidadeYard getCapacity(@RequestParam String zona) {
        return capacidadeYardService.getCapacidadePorZona(zona);
    }
}