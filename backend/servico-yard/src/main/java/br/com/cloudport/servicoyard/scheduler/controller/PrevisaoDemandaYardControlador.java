package br.com.cloudport.servicoyard.scheduler.controller;

import br.com.cloudport.servicoyard.scheduler.dto.PrevisaoDemandaYardDto;
import br.com.cloudport.servicoyard.scheduler.servico.PrevisaoDemandaYardServico;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler/previsao-demanda")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class PrevisaoDemandaYardControlador {

    private final PrevisaoDemandaYardServico servico;

    public PrevisaoDemandaYardControlador(PrevisaoDemandaYardServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public PrevisaoDemandaYardDto prever(
            @RequestParam(name = "horizonteHoras", defaultValue = "6") int horizonteHoras) {
        return servico.prever(horizonteHoras);
    }
}
