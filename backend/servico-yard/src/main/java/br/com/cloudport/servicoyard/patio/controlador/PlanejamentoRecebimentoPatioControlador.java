package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.PlanoRecebimentoPatioDto;
import br.com.cloudport.servicoyard.patio.servico.AgrupadorRecebimentoPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/planejamento-recebimento")
public class PlanejamentoRecebimentoPatioControlador {

    private final AgrupadorRecebimentoPatioServico agrupadorRecebimentoPatioServico;

    public PlanejamentoRecebimentoPatioControlador(
            AgrupadorRecebimentoPatioServico agrupadorRecebimentoPatioServico) {
        this.agrupadorRecebimentoPatioServico = agrupadorRecebimentoPatioServico;
    }

    @PostMapping
    public PlanoRecebimentoPatioDto planejar(
            @Valid @RequestBody List<@Valid ContainerOtimizacaoDto> conteineres) {
        return agrupadorRecebimentoPatioServico.planejar(conteineres);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErroDeNegocio(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
