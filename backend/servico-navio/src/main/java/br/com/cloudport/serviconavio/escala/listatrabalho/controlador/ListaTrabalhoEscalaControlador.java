package br.com.cloudport.serviconavio.escala.listatrabalho.controlador;

import br.com.cloudport.serviconavio.escala.listatrabalho.dto.AtualizacaoStatusOrdemNavioDTO;
import br.com.cloudport.serviconavio.escala.listatrabalho.dto.OrdemMovimentacaoNavioRespostaDTO;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.StatusOrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.servico.OrdemMovimentacaoNavioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/escalas/{idEscala}/ordens")
@Validated
public class ListaTrabalhoEscalaControlador {

    private final OrdemMovimentacaoNavioServico ordemServico;

    public ListaTrabalhoEscalaControlador(OrdemMovimentacaoNavioServico ordemServico) {
        this.ordemServico = ordemServico;
    }

    @GetMapping
    public List<OrdemMovimentacaoNavioRespostaDTO> listarOrdens(
            @PathVariable("idEscala") Long idEscala,
            @RequestParam(name = "status", required = false) StatusOrdemMovimentacaoNavio status) {
        return ordemServico.listarOrdensParaExecucao(idEscala, status);
    }

    @PatchMapping("/{idOrdem}/status")
    public OrdemMovimentacaoNavioRespostaDTO atualizarStatus(
            @PathVariable("idEscala") Long idEscala,
            @PathVariable("idOrdem") Long idOrdem,
            @Valid @RequestBody AtualizacaoStatusOrdemNavioDTO dto) {
        return ordemServico.atualizarStatus(idEscala, idOrdem, dto.getStatusMovimentacao());
    }
}
