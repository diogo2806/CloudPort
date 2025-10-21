package br.com.cloudport.servicorail.ferrovia.controlador;

import br.com.cloudport.servicorail.ferrovia.dto.AtualizacaoStatusOperacaoConteinerDto;
import br.com.cloudport.servicorail.ferrovia.dto.OperacaoConteinerVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicorail.ferrovia.servico.VisitaTremServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia/visitas")
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

    @PostMapping("/{id}/descarga")
    public VisitaTremRespostaDto adicionarConteinerDescarga(@PathVariable("id") Long id,
                                                            @Valid @RequestBody OperacaoConteinerVisitaRequisicaoDto dto) {
        return visitaTremServico.adicionarConteinerDescarga(id, dto);
    }

    @PostMapping("/{id}/carga")
    public VisitaTremRespostaDto adicionarConteinerCarga(@PathVariable("id") Long id,
                                                         @Valid @RequestBody OperacaoConteinerVisitaRequisicaoDto dto) {
        return visitaTremServico.adicionarConteinerCarga(id, dto);
    }

    @DeleteMapping("/{id}/descarga/{codigoConteiner}")
    public VisitaTremRespostaDto removerConteinerDescarga(@PathVariable("id") Long id,
                                                          @PathVariable("codigoConteiner") String codigoConteiner) {
        return visitaTremServico.removerConteinerDescarga(id, codigoConteiner);
    }

    @DeleteMapping("/{id}/carga/{codigoConteiner}")
    public VisitaTremRespostaDto removerConteinerCarga(@PathVariable("id") Long id,
                                                       @PathVariable("codigoConteiner") String codigoConteiner) {
        return visitaTremServico.removerConteinerCarga(id, codigoConteiner);
    }

    @PatchMapping("/{id}/descarga/{codigoConteiner}/status")
    public VisitaTremRespostaDto atualizarStatusDescarga(@PathVariable("id") Long id,
                                                         @PathVariable("codigoConteiner") String codigoConteiner,
                                                         @Valid @RequestBody AtualizacaoStatusOperacaoConteinerDto dto) {
        return visitaTremServico.atualizarStatusDescarga(id, codigoConteiner, dto.getStatusOperacao());
    }

    @PatchMapping("/{id}/carga/{codigoConteiner}/status")
    public VisitaTremRespostaDto atualizarStatusCarga(@PathVariable("id") Long id,
                                                      @PathVariable("codigoConteiner") String codigoConteiner,
                                                      @Valid @RequestBody AtualizacaoStatusOperacaoConteinerDto dto) {
        return visitaTremServico.atualizarStatusCarga(id, codigoConteiner, dto.getStatusOperacao());
    }
}
