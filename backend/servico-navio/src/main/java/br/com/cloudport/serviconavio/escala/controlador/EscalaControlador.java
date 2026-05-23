package br.com.cloudport.serviconavio.escala.controlador;

import br.com.cloudport.serviconavio.escala.dto.AtualizacaoEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.AvancarFaseEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.CadastroEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.EscalaDetalheDTO;
import br.com.cloudport.serviconavio.escala.dto.EscalaResumoDTO;
import br.com.cloudport.serviconavio.escala.servico.EscalaServico;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class EscalaControlador {

    private final EscalaServico escalaServico;

    public EscalaControlador(EscalaServico escalaServico) {
        this.escalaServico = escalaServico;
    }

    @GetMapping("/escalas")
    public List<EscalaResumoDTO> listarCronograma(@RequestParam(name = "dias", defaultValue = "7") int dias) {
        return escalaServico.listarCronograma(dias);
    }

    @GetMapping("/escalas/{id}")
    public EscalaDetalheDTO detalhar(@PathVariable Long id) {
        return escalaServico.buscarDetalhe(id);
    }

    @PutMapping("/escalas/{id}")
    public EscalaDetalheDTO atualizar(@PathVariable Long id,
                                      @Valid @RequestBody AtualizacaoEscalaDTO dto) {
        return escalaServico.atualizar(id, dto);
    }

    @PatchMapping("/escalas/{id}/fase")
    public EscalaDetalheDTO avancarFase(@PathVariable Long id,
                                        @Valid @RequestBody AvancarFaseEscalaDTO dto) {
        return escalaServico.avancarFase(id, dto.getFase());
    }

    @DeleteMapping("/escalas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        escalaServico.remover(id);
    }

    @GetMapping("/navios/{navioId}/escalas")
    public List<EscalaResumoDTO> listarPorNavio(@PathVariable Long navioId) {
        return escalaServico.listarPorNavio(navioId);
    }

    @PostMapping("/navios/{navioId}/escalas")
    @ResponseStatus(HttpStatus.CREATED)
    public EscalaDetalheDTO registrar(@PathVariable Long navioId,
                                      @Valid @RequestBody CadastroEscalaDTO dto) {
        return escalaServico.registrar(navioId, dto);
    }
}
