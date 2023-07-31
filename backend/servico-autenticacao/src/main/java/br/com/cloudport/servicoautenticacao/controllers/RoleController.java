package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.dto.RoleDTO; // Note o "R" maiúsculo
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
    public ResponseEntity<RoleDTO> createRole(@RequestBody RoleDTO roleDTO) { // Note o "R" maiúsculo
        RoleDTO savedRole = roleService.saveRole(roleDTO); // Note o "R" maiúsculo
        return ResponseEntity.ok(savedRole);
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoleDTO> getRole(@PathVariable String name) { // Note o "R" maiúsculo
        RoleDTO role = roleService.findByName(name); // Note o "R" maiúsculo
        return ResponseEntity.ok(role);
    }
}
