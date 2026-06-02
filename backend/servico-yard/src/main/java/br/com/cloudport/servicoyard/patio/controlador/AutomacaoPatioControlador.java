package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.RespostaAutoplanejamentoDto;
import br.com.cloudport.servicoyard.patio.servico.AutomacaoPatioServico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard/patio/automacao")
public class AutomacaoPatioControlador {

    private final AutomacaoPatioServico automacaoPatioServico;

    public AutomacaoPatioControlador(AutomacaoPatioServico automacaoPatioServico) {
        this.automacaoPatioServico = automacaoPatioServico;
    }

    @PostMapping("/executar-autoplanejamento")
    public ResponseEntity<RespostaAutoplanejamentoDto> executarAutoplanejamento() {
        var resposta = automacaoPatioServico.executarAutoplanejamento();
        return ResponseEntity.ok(resposta);
    }
}
