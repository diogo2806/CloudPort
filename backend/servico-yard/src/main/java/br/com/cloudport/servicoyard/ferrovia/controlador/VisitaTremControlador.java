package br.com.cloudport.servicoyard.ferrovia.controlador;

import br.com.cloudport.servicoyard.ferrovia.dto.VisitaTremRequisicaoDto;
import br.com.cloudport.servicoyard.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicoyard.ferrovia.servico.VisitaTremServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/ferrovia/visitas")
public class VisitaTremControlador {

    private final VisitaTremServico visitaTremServico;

    public VisitaTremControlador(VisitaTremServico visitaTremServico) {
        this.visitaTremServico = visitaTremServico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaTremRespostaDto registrar(@Valid @RequestBody VisitaTremRequisicaoDto dto) {
        return visitaTremServico.registrarVisita(dto);
    }

    @PutMapping("/{id}")
    public VisitaTremRespostaDto atualizar(@PathVariable("id") Long id,
                                           @Valid @RequestBody VisitaTremRequisicaoDto dto) {
        return visitaTremServico.atualizarVisita(id, dto);
    }

    @GetMapping("/{id}")
    public VisitaTremRespostaDto consultar(@PathVariable("id") Long id) {
        return visitaTremServico.consultarVisita(id);
    }

    @GetMapping
    public List<VisitaTremRespostaDto> listar(@RequestParam(name = "dias", defaultValue = "7") int dias) {
        return visitaTremServico.listarVisitasProximosDias(dias);
    }
}
