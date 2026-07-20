package br.com.cloudport.servicoyard.scheduler.controller;

import br.com.cloudport.servicoyard.scheduler.dto.YardImpactRespostaDto;
import br.com.cloudport.servicoyard.scheduler.servico.YardImpactServico;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler/yard-impact")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class YardImpactControlador {

    private final YardImpactServico servico;

    public YardImpactControlador(YardImpactServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public YardImpactRespostaDto projetar(
            @RequestParam(name = "horizonteHoras", defaultValue = "6") Integer horizonteHoras) {
        return servico.projetar(horizonteHoras);
    }
}
