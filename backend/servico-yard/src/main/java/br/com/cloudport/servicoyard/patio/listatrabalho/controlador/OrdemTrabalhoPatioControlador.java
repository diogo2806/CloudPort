package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoStatusOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EstatisticasOtimizacaoRotaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OtimizadorRotasPatioServico;
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
    private final OtimizadorRotasPatioServico otimizadorRotas;

    public OrdemTrabalhoPatioControlador(OrdemTrabalhoPatioServico ordemTrabalhoPatioServico,
                                         OtimizadorRotasPatioServico otimizadorRotas) {
        this.ordemTrabalhoPatioServico = ordemTrabalhoPatioServico;
        this.otimizadorRotas = otimizadorRotas;
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

    @GetMapping("/otimizacao/nearest-neighbor")
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensOtimizadas() {
        return ordemTrabalhoPatioServico.listarOrdensOtimizadas(StatusOrdemTrabalhoPatio.PENDENTE);
    }

    @GetMapping("/otimizacao/dual-cycling")
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensComDualCycling() {
        return ordemTrabalhoPatioServico.listarOrdensOtimizadasComDualCycling();
    }

    @GetMapping("/otimizacao/estatisticas")
    public EstatisticasOtimizacaoRotaDto obterEstatisticasOtimizacao() {
        List<OrdemTrabalhoPatio> ordensOriginais = ordemTrabalhoPatioServico
                .listarOrdensOriginais(StatusOrdemTrabalhoPatio.PENDENTE);
        List<OrdemTrabalhoPatio> ordensOtimizadas = otimizadorRotas.otimizarRota();

        var stats = otimizadorRotas.obterEstatisticasOtimizacao(ordensOriginais, ordensOtimizadas);

        return new EstatisticasOtimizacaoRotaDto(
                (Integer) stats.get("totalOrdens"),
                (Double) stats.get("distanciaOriginal"),
                (Double) stats.get("distanciaOtimizada"),
                (Double) stats.get("percentualMejora"),
                ordensOtimizadas.stream()
                        .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                        .toList()
        );
    }
}
