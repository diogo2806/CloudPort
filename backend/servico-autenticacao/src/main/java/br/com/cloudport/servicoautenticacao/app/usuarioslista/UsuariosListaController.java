package br.com.cloudport.servicoautenticacao.app.usuarioslista;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.dto.UsuarioResumoDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosListaController {

    private final UsuariosListaServico usuariosListaServico;

    public UsuariosListaController(UsuariosListaServico usuariosListaServico) {
        this.usuariosListaServico = usuariosListaServico;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<List<UsuarioResumoDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuariosListaServico.listarUsuariosResumo());
    }
}
