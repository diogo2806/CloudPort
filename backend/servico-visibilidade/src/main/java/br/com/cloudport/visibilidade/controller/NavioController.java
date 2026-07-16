package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibilidade/navios")
public class NavioController {

    private final StatusNavioService statusNavioService;

    public NavioController(StatusNavioService statusNavioService) {
        this.statusNavioService = statusNavioService;
    }

    @GetMapping("/{navioId}/status")
    public StatusNavioDTO getStatus(@PathVariable String navioId) {
        return statusNavioService.getStatusNavio(navioId);
    }
}
