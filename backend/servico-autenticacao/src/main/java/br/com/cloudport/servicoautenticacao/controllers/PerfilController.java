package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.domain.user.Perfil;
import br.com.cloudport.servicoautenticacao.service.PerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class PerfilController {
//tesye
    private final PerfilService perfilService;

    @Autowired
    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Perfil perfil) {
        Perfil savedRole = perfilService.savePrefil(perfil);
        return ResponseEntity.ok(savedPrefil);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Role> getRole(@PathVariable String name) {
        Perfil perfil = perfilService.findByName(name);
        return ResponseEntity.ok(prefil);
    }
}
