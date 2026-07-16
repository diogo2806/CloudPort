package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.PosicaoReservaPatioDto;
import br.com.cloudport.servicoyard.patio.servico.ConsultaReservaPatioServico;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/reservas")
public class ReservaPatioControlador {

    private final ConsultaReservaPatioServico consultaReservaPatioServico;

    public ReservaPatioControlador(ConsultaReservaPatioServico consultaReservaPatioServico) {
        this.consultaReservaPatioServico = consultaReservaPatioServico;
    }

    @GetMapping("/posicoes")
    public List<PosicaoReservaPatioDto> listarPosicoesReservaveis() {
        return consultaReservaPatioServico.listarPosicoesReservaveis();
    }
}
