package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoStatusOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/ordens")
public class OrdemTrabalhoPatioControlador {

    private final OrdemTrabalhoPatioServico ordemTrabalhoPatioServico;

    public OrdemTrabalhoPatioControlador(OrdemTrabalhoPatioServico ordemTrabalhoPatioServico) {
        this.ordemTrabalhoPatioServico = ordemTrabalhoPatioServico;
    }

    @GetMapping
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdens(@RequestParam(name = "status", required = false) StatusOrdemTrabalhoPatio status) {
        return ordemTrabalhoPatioServico.listarOrdens(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemTrabalhoPatioRespostaDto registrarOrdem(@Valid @RequestBody OrdemTrabalhoPatioRequisicaoDto dto) {
        return ordemTrabalhoPatioServico.registrarOrdem(dto);
    }

    @PatchMapping("/{id}/status")
    public OrdemTrabalhoPatioRespostaDto atualizarStatus(@PathVariable("id") Long id,
                                                         @Valid @RequestBody AtualizacaoStatusOrdemTrabalhoDto dto) {
        return ordemTrabalhoPatioServico.atualizarStatus(id, dto);
    }
}
