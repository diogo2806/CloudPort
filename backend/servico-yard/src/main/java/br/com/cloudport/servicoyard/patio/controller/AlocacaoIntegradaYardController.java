package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.RespostaAlocacaoIntegradaYardDto;
import br.com.cloudport.servicoyard.patio.servico.AlocacaoIntegradaYardServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patio/otimizacao")
public class AlocacaoIntegradaYardController {

    private final AlocacaoIntegradaYardServico servico;

    public AlocacaoIntegradaYardController(AlocacaoIntegradaYardServico servico) {
        this.servico = servico;
    }

    @PostMapping("/alocar-e-validar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public ResponseEntity<RespostaAlocacaoIntegradaYardDto> alocarEValidar(
            @Valid @RequestBody List<ContainerOtimizacaoDto> containers) {
        return ResponseEntity.ok(servico.alocar(containers));
    }
}
