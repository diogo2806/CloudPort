package br.com.cloudport.servicorail.ferrovia.controlador;

import br.com.cloudport.servicorail.ferrovia.dto.TremCadastroDto;
import br.com.cloudport.servicorail.ferrovia.servico.TremCadastroServico;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia/trens")
public class TremCadastroControlador {
    private final TremCadastroServico servico;
    public TremCadastroControlador(TremCadastroServico servico) { this.servico = servico; }

    @GetMapping
    public List<TremCadastroDto> listar() { return servico.listar(); }

    @GetMapping("/{id}")
    public TremCadastroDto consultar(@PathVariable Long id) { return servico.consultar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TremCadastroDto criar(@Valid @RequestBody TremCadastroDto dto, Principal principal) {
        return servico.criar(dto, usuario(principal));
    }

    @PutMapping("/{id}")
    public TremCadastroDto atualizar(@PathVariable Long id, @Valid @RequestBody TremCadastroDto dto, Principal principal) {
        return servico.atualizar(id, dto, usuario(principal));
    }

    @PatchMapping("/{id}/situacao")
    public TremCadastroDto alterarSituacao(@PathVariable Long id, @RequestParam boolean ativo, Principal principal) {
        return servico.alterarSituacao(id, ativo, usuario(principal));
    }

    private String usuario(Principal principal) { return principal == null ? "sistema" : principal.getName(); }
}
