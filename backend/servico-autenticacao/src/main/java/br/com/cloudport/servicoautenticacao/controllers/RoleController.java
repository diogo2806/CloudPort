package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.domain.user.Role;
import br.com.cloudport.servicoautenticacao.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Role savedRole = roleService.saveRole(role);
        return ResponseEntity.ok(savedRole);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Role> getRole(@PathVariable String name) {
        Role role = roleService.findByName(name);
        return ResponseEntity.ok(role);
    }
}
