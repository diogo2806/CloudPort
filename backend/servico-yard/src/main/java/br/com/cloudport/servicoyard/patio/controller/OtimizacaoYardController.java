package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoOtimizadaDto;
import br.com.cloudport.servicoyard.patio.dto.RespostaAlocacaoIntegradaYardDto;
import br.com.cloudport.servicoyard.patio.servico.AlocacaoIntegradaYardServico;
import br.com.cloudport.servicoyard.patio.servico.OptimizadorYardService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patio/otimizacao")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
public class OtimizacaoYardController {

    private final OptimizadorYardService otimizadorYard;
    private final AlocacaoIntegradaYardServico alocacaoIntegrada;

    public OtimizacaoYardController(OptimizadorYardService otimizadorYard,
                                     AlocacaoIntegradaYardServico alocacaoIntegrada) {
        this.otimizadorYard = otimizadorYard;
        this.alocacaoIntegrada = alocacaoIntegrada;
    }

    @PostMapping("/alocar")
    public ResponseEntity<List<PosicaoOtimizadaDto>> otimizarAlocacao(
            @Valid @RequestBody List<ContainerOtimizacaoDto> containers) {
        return ResponseEntity.status(HttpStatus.OK).body(otimizadorYard.otimizarAlocacao(containers));
    }

    @PostMapping("/alocar-por-navio")
    public ResponseEntity<List<PosicaoOtimizadaDto>> otimizarAlocacaoPorNavio(
            @Valid @RequestBody List<ContainerOtimizacaoDto> containers,
            @RequestParam(defaultValue = "999999") Integer distanciaMaximaAoBerco) {
        return ResponseEntity.status(HttpStatus.OK).body(
                otimizadorYard.otimizarAlocacaoPorNavio(containers, distanciaMaximaAoBerco));
    }

    @PostMapping("/alocar-e-validar")
    public ResponseEntity<RespostaAlocacaoIntegradaYardDto> alocarEValidar(
            @Valid @RequestBody List<ContainerOtimizacaoDto> containers) {
        return ResponseEntity.ok(alocacaoIntegrada.alocar(containers));
    }
}
