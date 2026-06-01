package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/visibilidade/navios")
public class NavioController {

    @Autowired
    private StatusNavioService statusNavioService;

    @GetMapping("/{navioId}/status")
    public StatusNavioDTO getStatus(@PathVariable String navioId) {
        return statusNavioService.getStatusNavio(navioId);
    }

    @GetMapping
    public List<StatusNavioDTO> listarEmOperacao() {
        return statusNavioService.listarNaviosEmOperacao();
    }
}