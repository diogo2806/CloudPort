package br.com.cloudport.servicorail.ferrovia.controlador;

import br.com.cloudport.servicorail.ferrovia.dto.TremMestreRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.TremMestreRespostaDto;
import br.com.cloudport.servicorail.ferrovia.servico.TremMestreServico;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class TremMestreControlador {
    private final TremMestreServico servico;
    public TremMestreControlador(TremMestreServico servico) { this.servico = servico; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TremMestreRespostaDto> listar() { return servico.listar(); }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TremMestreRespostaDto consultar(@PathVariable Long id) { return servico.consultar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public TremMestreRespostaDto criar(@Valid @RequestBody TremMestreRequisicaoDto dto, Principal principal) { return servico.criar(dto, usuario(principal)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public TremMestreRespostaDto atualizar(@PathVariable Long id, @Valid @RequestBody TremMestreRequisicaoDto dto, Principal principal) { return servico.atualizar(id, dto, usuario(principal)); }

    @PatchMapping("/{id}/situacao")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public TremMestreRespostaDto alterarSituacao(@PathVariable Long id, @RequestParam boolean ativo, Principal principal) { return servico.alterarSituacao(id, ativo, usuario(principal)); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public void excluir(@PathVariable Long id) { servico.excluir(id); }

    private String usuario(Principal principal) { return principal != null ? principal.getName() : "sistema"; }
}
