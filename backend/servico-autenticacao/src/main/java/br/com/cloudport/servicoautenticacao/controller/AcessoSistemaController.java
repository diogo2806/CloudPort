package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.AcessoSistemaDTO;
import br.com.cloudport.servicoautenticacao.model.AcessoSistema;
import br.com.cloudport.servicoautenticacao.service.AcessoSistemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/acessos-sistema")
public class AcessoSistemaController {

    private final AcessoSistemaService acessoSistemaService;

    @Autowired
    public AcessoSistemaController(AcessoSistemaService acessoSistemaService) {
        this.acessoSistemaService = acessoSistemaService;
    }

    @GetMapping
    public ResponseEntity<List<AcessoSistemaDTO>> listarTodosAcessosSistema() {
        List<AcessoSistemaDTO> acessosSistema = acessoSistemaService.listarTodosAcessosSistema();
        return ResponseEntity.ok(acessosSistema);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcessoSistemaDTO> encontrarAcessoSistemaPorId(@PathVariable Long id) {
        AcessoSistemaDTO acessoSistema = acessoSistemaService.encontrarAcessoSistemaPorId(id);
        if (acessoSistema != null) {
            return ResponseEntity.ok(acessoSistema);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<AcessoSistemaDTO> salvarAcessoSistema(@RequestBody AcessoSistemaDTO acessoSistemaDTO) {
        AcessoSistemaDTO acessoSistemaSalvo = acessoSistemaService.salvarAcessoSistema(acessoSistemaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(acessoSistemaSalvo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAcessoSistema(@PathVariable Long id) {
        acessoSistemaService.deletarAcessoSistema(id);
        return ResponseEntity.noContent().build();
    }
}
