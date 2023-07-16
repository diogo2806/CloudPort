package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.PerfilAcessoDTO;
import br.com.cloudport.servicoautenticacao.service.PerfilAcessoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfis")
public class PerfilAcessoController {

    private final PerfilAcessoService perfilAcessoService;

    public PerfilAcessoController(PerfilAcessoService perfilAcessoService) {
        this.perfilAcessoService = perfilAcessoService;
    }

    @GetMapping
    public ResponseEntity<List<PerfilAcessoDTO>> listarTodosPerfis() {
        List<PerfilAcessoDTO> perfis = perfilAcessoService.listarTodosPerfis();
        return new ResponseEntity<>(perfis, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfilAcessoDTO> encontrarPerfilPorId(@PathVariable Long id) {
        PerfilAcessoDTO perfil = perfilAcessoService.encontrarPerfilPorId(id);
        if (perfil == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(perfil, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<PerfilAcessoDTO> salvarPerfil(@RequestBody PerfilAcessoDTO novoPerfil) {
        PerfilAcessoDTO perfil = perfilAcessoService.salvarPerfil(novoPerfil);
        return new ResponseEntity<>(perfil, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPerfil(@PathVariable Long id) {
        perfilAcessoService.deletarPerfil(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
