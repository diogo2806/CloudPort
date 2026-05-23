package br.com.cloudport.serviconavio.atracacao.controlador;

import br.com.cloudport.serviconavio.atracacao.dto.AtualizacaoBercoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.BercoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.CadastroBercoDTO;
import br.com.cloudport.serviconavio.atracacao.servico.BercoServico;
import java.util.List;
import javax.validation.Valid;
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

@RestController
@RequestMapping("/bercos")
@Validated
public class BercoControlador {

    private final BercoServico bercoServico;

    public BercoControlador(BercoServico bercoServico) {
        this.bercoServico = bercoServico;
    }

    @GetMapping
    public List<BercoDTO> listar() {
        return bercoServico.listar();
    }

    @GetMapping("/{identificador}")
    public BercoDTO detalhar(@PathVariable Long identificador) {
        return bercoServico.buscar(identificador);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BercoDTO registrar(@Valid @RequestBody CadastroBercoDTO dto) {
        return bercoServico.registrar(dto);
    }

    @PutMapping("/{identificador}")
    public BercoDTO atualizar(@PathVariable Long identificador, @Valid @RequestBody AtualizacaoBercoDTO dto) {
        return bercoServico.atualizar(identificador, dto);
    }

    @DeleteMapping("/{identificador}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long identificador) {
        bercoServico.remover(identificador);
    }
}
