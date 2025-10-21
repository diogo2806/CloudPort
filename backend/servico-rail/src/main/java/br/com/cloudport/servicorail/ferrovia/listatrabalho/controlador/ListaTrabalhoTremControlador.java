package br.com.cloudport.servicorail.ferrovia.listatrabalho.controlador;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.dto.AtualizacaoStatusOrdemMovimentacaoDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.dto.OrdemMovimentacaoRespostaDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.servico.OrdemMovimentacaoServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia/lista-trabalho")
public class ListaTrabalhoTremControlador {

    private final OrdemMovimentacaoServico ordemMovimentacaoServico;

    public ListaTrabalhoTremControlador(OrdemMovimentacaoServico ordemMovimentacaoServico) {
        this.ordemMovimentacaoServico = ordemMovimentacaoServico;
    }

    @GetMapping("/visitas/{idVisita}/ordens")
    public List<OrdemMovimentacaoRespostaDto> listarOrdens(@PathVariable("idVisita") Long idVisita,
                                                           @RequestParam(name = "status", required = false)
                                                           StatusOrdemMovimentacao status) {
        return ordemMovimentacaoServico.listarOrdensParaExecucao(idVisita, status);
    }

    @PatchMapping("/visitas/{idVisita}/ordens/{idOrdem}/status")
    public OrdemMovimentacaoRespostaDto atualizarStatus(@PathVariable("idVisita") Long idVisita,
                                                         @PathVariable("idOrdem") Long idOrdem,
                                                         @Valid @RequestBody AtualizacaoStatusOrdemMovimentacaoDto dto) {
        return ordemMovimentacaoServico.atualizarStatus(idVisita, idOrdem, dto.getStatusMovimentacao());
    }
}
