package br.com.cloudport.servicoyard.patio.seguranca;

import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Criar;
import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Liberar;
import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Prorrogar;
import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Resposta;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/zonas-seguranca")
public class ZonaSegurancaPatioControlador {

    private final ZonaSegurancaPatioServico servico;

    public ZonaSegurancaPatioControlador(ZonaSegurancaPatioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Resposta> listar() {
        return servico.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resposta criar(@Valid @RequestBody Criar dto) {
        return servico.criar(dto);
    }

    @PostMapping("/{id}/ativar")
    public Resposta ativar(@PathVariable Long id,
                           @RequestParam(required = false) String operador,
                           @RequestParam(required = false) String correlationId) {
        return servico.ativar(id, operador, correlationId);
    }

    @PostMapping("/{id}/prorrogar")
    public Resposta prorrogar(@PathVariable Long id, @Valid @RequestBody Prorrogar dto) {
        return servico.prorrogar(id, dto);
    }

    @PostMapping("/{id}/liberar")
    public Resposta liberar(@PathVariable Long id, @Valid @RequestBody Liberar dto) {
        return servico.liberar(id, dto);
    }
}
