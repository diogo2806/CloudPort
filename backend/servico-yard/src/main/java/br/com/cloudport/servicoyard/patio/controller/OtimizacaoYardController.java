package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoOtimizadaDto;
import br.com.cloudport.servicoyard.patio.servico.OptimizadorYardService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patio/otimizacao")
public class OtimizacaoYardController {

    private final OptimizadorYardService otimizadorYard;

    public OtimizacaoYardController(OptimizadorYardService otimizadorYard) {
        this.otimizadorYard = otimizadorYard;
    }

    @PostMapping("/alocar")
    public ResponseEntity<List<PosicaoOtimizadaDto>> otimizarAlocacao(
            @Valid @RequestBody List<ContainerOtimizacaoDto> containers) {

        List<PosicaoOtimizadaDto> posicoes = otimizadorYard.otimizarAlocacao(containers);
        return ResponseEntity.status(HttpStatus.OK).body(posicoes);
    }

    @PostMapping("/alocar-por-navio")
    public ResponseEntity<List<PosicaoOtimizadaDto>> otimizarAlocacaoPorNavio(
            @Valid @RequestBody List<ContainerOtimizacaoDto> containers,
            @RequestParam(defaultValue = "999999") Integer distanciaMaximaAoBerco) {

        List<PosicaoOtimizadaDto> posicoes = otimizadorYard.otimizarAlocacaoPorNavio(
                containers,
                distanciaMaximaAoBerco
        );
        return ResponseEntity.status(HttpStatus.OK).body(posicoes);
    }
}
