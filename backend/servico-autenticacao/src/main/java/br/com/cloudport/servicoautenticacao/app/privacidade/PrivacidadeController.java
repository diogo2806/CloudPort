package br.com.cloudport.servicoautenticacao.app.privacidade;

import br.com.cloudport.servicoautenticacao.app.privacidade.dto.OpcaoPrivacidadeRespostaDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracoes/privacidade")
public class PrivacidadeController {

    private final PrivacidadeConsultaServico privacidadeConsultaServico;

    public PrivacidadeController(PrivacidadeConsultaServico privacidadeConsultaServico) {
        this.privacidadeConsultaServico = privacidadeConsultaServico;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<List<OpcaoPrivacidadeRespostaDTO>> listarOpcoes() {
        return ResponseEntity.ok(privacidadeConsultaServico.listarOpcoes());
    }
}
