package br.com.cloudport.servicogate.app.administracao;

import br.com.cloudport.servicogate.app.administracao.dto.ContingenciaAgendamentoRequest;
import br.com.cloudport.servicogate.app.administracao.dto.ContingenciaLiberacaoRequest;
import br.com.cloudport.servicogate.app.administracao.dto.ContingenciaResponse;
import javax.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/contingencia")
@Validated
@ConditionalOnProperty(prefix = "cloudport.gate.contingencia", name = "enabled", havingValue = "true")
public class ContingenciaController {

    private final ContingenciaService contingenciaService;

    public ContingenciaController(ContingenciaService contingenciaService) {
        this.contingenciaService = contingenciaService;
    }

    @PostMapping("/agendar")
    public ResponseEntity<ContingenciaResponse> agendar(@Valid @RequestBody ContingenciaAgendamentoRequest request) {
        ContingenciaResponse response = contingenciaService.agendar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/liberar")
    public ResponseEntity<ContingenciaResponse> liberar(@Valid @RequestBody ContingenciaLiberacaoRequest request) {
        ContingenciaResponse response = contingenciaService.liberar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
