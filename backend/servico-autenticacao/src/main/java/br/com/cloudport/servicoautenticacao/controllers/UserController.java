package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.app.administracao.dto.UserSummaryDTO;
import br.com.cloudport.servicoautenticacao.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<List<UserSummaryDTO>> listarUsuarios() {
        return ResponseEntity.ok(userService.listarUsuariosResumo());
    }
}
