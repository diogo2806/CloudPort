package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.StatusPatioDto;
import br.com.cloudport.servicoyard.patio.servico.StatusPatioServico;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/status")
public class StatusPatioControlador {

    private final StatusPatioServico statusPatioServico;

    public StatusPatioControlador(StatusPatioServico statusPatioServico) {
        this.statusPatioServico = statusPatioServico;
    }

    @GetMapping
    public StatusPatioDto verificarStatus() {
        return statusPatioServico.verificarDisponibilidade();
    }
}
