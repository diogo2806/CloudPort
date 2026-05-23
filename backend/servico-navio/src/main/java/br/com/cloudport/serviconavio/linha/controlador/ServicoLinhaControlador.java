package br.com.cloudport.serviconavio.linha.controlador;

import br.com.cloudport.serviconavio.linha.dto.AtualizacaoServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.dto.CadastroServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.dto.ServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.servico.ServicoLinhaServico;
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
@RequestMapping("/servicos-linha")
@Validated
public class ServicoLinhaControlador {

    private final ServicoLinhaServico servicoLinhaServico;

    public ServicoLinhaControlador(ServicoLinhaServico servicoLinhaServico) {
        this.servicoLinhaServico = servicoLinhaServico;
    }

    @GetMapping
    public List<ServicoLinhaDTO> listar() {
        return servicoLinhaServico.listar();
    }

    @GetMapping("/{identificador}")
    public ServicoLinhaDTO detalhar(@PathVariable Long identificador) {
        return servicoLinhaServico.buscar(identificador);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoLinhaDTO registrar(@Valid @RequestBody CadastroServicoLinhaDTO dto) {
        return servicoLinhaServico.registrar(dto);
    }

    @PutMapping("/{identificador}")
    public ServicoLinhaDTO atualizar(@PathVariable Long identificador,
                                     @Valid @RequestBody AtualizacaoServicoLinhaDTO dto) {
        return servicoLinhaServico.atualizar(identificador, dto);
    }

    @DeleteMapping("/{identificador}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long identificador) {
        servicoLinhaServico.remover(identificador);
    }
}
