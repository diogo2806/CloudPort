package br.com.cloudport.servicoautenticacao.app.navegacao;

import br.com.cloudport.servicoautenticacao.app.navegacao.dto.AbaNavegacaoDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/navegacao/abas")
public class NavegacaoAbasControlador {

    private final NavegacaoConsultaServico navegacaoConsultaServico;

    public NavegacaoAbasControlador(NavegacaoConsultaServico navegacaoConsultaServico) {
        this.navegacaoConsultaServico = navegacaoConsultaServico;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AbaNavegacaoDTO>> listarAbas() {
        return ResponseEntity.ok(navegacaoConsultaServico.listarAbas());
    }
}
