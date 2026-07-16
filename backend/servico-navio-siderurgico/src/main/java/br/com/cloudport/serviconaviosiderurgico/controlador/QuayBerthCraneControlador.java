package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ProdutividadeCaisDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.QuayMonitorDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.QuayBerthCraneServico;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visitas-navio/{id}")
public class QuayBerthCraneControlador {

    private final QuayBerthCraneServico quayBerthCraneServico;

    public QuayBerthCraneControlador(QuayBerthCraneServico quayBerthCraneServico) {
        this.quayBerthCraneServico = quayBerthCraneServico;
    }

    @GetMapping("/quay-monitor")
    public QuayMonitorDTO obterQuayMonitor(@PathVariable Long id) {
        return quayBerthCraneServico.obterQuayMonitor(id);
    }

    @PostMapping("/crane-plan")
    public PlanoGuindasteDTO salvarPlanoGuindaste(
            @PathVariable Long id,
            @Valid @RequestBody ComandoPlanoGuindasteDTO comando
    ) {
        return quayBerthCraneServico.salvarPlano(id, comando);
    }

    @GetMapping("/produtividade-cais")
    public ProdutividadeCaisDTO obterProdutividadeCais(@PathVariable Long id) {
        return quayBerthCraneServico.obterProdutividadeCais(id);
    }
}
