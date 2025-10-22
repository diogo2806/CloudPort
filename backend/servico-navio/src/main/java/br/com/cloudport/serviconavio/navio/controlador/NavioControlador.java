package br.com.cloudport.serviconavio.navio.controlador;

import br.com.cloudport.serviconavio.navio.dto.AtualizacaoNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.CadastroNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.servico.NavioServico;
import org.springframework.http.HttpStatus;
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

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/navios")
@Validated
public class NavioControlador {

    private final NavioServico navioServico;

    public NavioControlador(NavioServico navioServico) {
        this.navioServico = navioServico;
    }

    @GetMapping
    public List<NavioResumoDTO> listar() {
        return navioServico.listarResumo();
    }

    @GetMapping("/{identificador}")
    public NavioDetalheDTO detalhar(@PathVariable Long identificador) {
        return navioServico.buscarDetalhe(identificador);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NavioDetalheDTO registrar(@Valid @RequestBody CadastroNavioDTO dto) {
        return navioServico.registrar(dto);
    }

    @PutMapping("/{identificador}")
    public NavioDetalheDTO atualizar(@PathVariable Long identificador,
                                     @Valid @RequestBody AtualizacaoNavioDTO dto) {
        return navioServico.atualizar(identificador, dto);
    }

    @DeleteMapping("/{identificador}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long identificador) {
        navioServico.remover(identificador);
    }
}
