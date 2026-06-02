package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.CenarioSimulacaoDto;
import br.com.cloudport.servicoyard.patio.dto.ResultadoSimulacaoDto;
import br.com.cloudport.servicoyard.patio.servico.SimuladorPatioServico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard/patio/simulador")
public class SimuladorPatioControlador {

    private final SimuladorPatioServico simuladorPatioServico;

    public SimuladorPatioControlador(SimuladorPatioServico simuladorPatioServico) {
        this.simuladorPatioServico = simuladorPatioServico;
    }

    @PostMapping("/simular")
    public ResponseEntity<ResultadoSimulacaoDto> simularCenario(@RequestBody CenarioSimulacaoDto cenario) {
        var resultado = simuladorPatioServico.simularCenario(cenario);
        return ResponseEntity.ok(resultado);
    }
}
