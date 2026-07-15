package br.com.cloudport.serviconavio.navio.controlador;

import br.com.cloudport.serviconavio.navio.dto.AtualizacaoNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.CadastroNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.servico.NavioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/navios")
@Validated
public class NavioControlador {

    private final NavioServico navioServico;

    public NavioControlador(NavioServico navioServico) {
        this.navioServico = navioServico;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','SERVICE_SIDERURGICO')")
    public List<NavioResumoDTO> listar() {
        return navioServico.listarResumo();
    }

    @GetMapping("/{identificador}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','SERVICE_SIDERURGICO')")
    public NavioDetalheDTO detalhar(@PathVariable Long identificador) {
        return navioServico.buscarDetalhe(identificador);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public NavioDetalheDTO registrar(@Valid @RequestBody CadastroNavioDTO dto) {
        return navioServico.registrar(dto);
    }

    @PutMapping("/{identificador}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public NavioDetalheDTO atualizar(@PathVariable Long identificador,
                                      @Valid @RequestBody AtualizacaoNavioDTO dto) {
        return navioServico.atualizar(identificador, dto);
    }

    @DeleteMapping("/{identificador}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public void remover(@PathVariable Long identificador) {
        navioServico.remover(identificador);
    }
}
