package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.service.RastreamentoConteinerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/visibilidade/conteiners")
public class ConteinerController {

    @Autowired
    private RastreamentoConteinerService rastreamentoService;

    @GetMapping("/{containerId}/track")
    public ConteinerRastreamentoDTO rastrear(@PathVariable String containerId) {
        return rastreamentoService.rastrearContainer(containerId);
    }

    @GetMapping("/{containerId}/historico")
    public List<HistoricoMovimento> historico(@PathVariable String containerId) {
        return rastreamentoService.obterHistorico(containerId);
    }

    @GetMapping("/buscar")
    public List<?> buscar(
            @RequestParam(required = false) String containerId,
            @RequestParam(required = false) String statusAtual,
            @RequestParam(required = false) String zonaYard,
            @RequestParam(required = false) String navioDestino) {
        return rastreamentoService.buscarContainers(containerId, statusAtual, zonaYard, navioDestino);
    }
}