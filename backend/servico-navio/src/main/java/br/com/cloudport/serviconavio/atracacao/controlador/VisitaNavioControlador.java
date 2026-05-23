package br.com.cloudport.serviconavio.atracacao.controlador;

import br.com.cloudport.serviconavio.atracacao.dto.AtualizacaoStatusOperacaoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.AtualizacaoVisitaNavioDTO;
import br.com.cloudport.serviconavio.atracacao.dto.CadastroVisitaNavioDTO;
import br.com.cloudport.serviconavio.atracacao.dto.OperacaoNavioConteinerDTO;
import br.com.cloudport.serviconavio.atracacao.dto.OperacaoNavioConteinerRequest;
import br.com.cloudport.serviconavio.atracacao.dto.PlanejamentoAtracacaoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.VisitaNavioDetalheDTO;
import br.com.cloudport.serviconavio.atracacao.dto.VisitaNavioResumoDTO;
import br.com.cloudport.serviconavio.atracacao.servico.VisitaNavioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visitas")
@Validated
public class VisitaNavioControlador {

    private final VisitaNavioServico visitaNavioServico;

    public VisitaNavioControlador(VisitaNavioServico visitaNavioServico) {
        this.visitaNavioServico = visitaNavioServico;
    }

    @GetMapping
    public List<VisitaNavioResumoDTO> listar() {
        return visitaNavioServico.listar();
    }

    @GetMapping("/agenda-atracacao")
    public List<VisitaNavioResumoDTO> listarAgendaDeAtracacao() {
        return visitaNavioServico.listarAgendaDeAtracacao();
    }

    @GetMapping("/{identificador}")
    public VisitaNavioDetalheDTO detalhar(@PathVariable Long identificador) {
        return visitaNavioServico.buscarDetalhe(identificador);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaNavioDetalheDTO registrar(@Valid @RequestBody CadastroVisitaNavioDTO dto) {
        return visitaNavioServico.registrar(dto);
    }

    @PutMapping("/{identificador}")
    public VisitaNavioDetalheDTO atualizar(@PathVariable Long identificador,
                                           @Valid @RequestBody AtualizacaoVisitaNavioDTO dto) {
        return visitaNavioServico.atualizar(identificador, dto);
    }

    @PutMapping("/{identificador}/atracacao")
    public VisitaNavioDetalheDTO planejarAtracacao(@PathVariable Long identificador,
                                                   @Valid @RequestBody PlanejamentoAtracacaoDTO dto) {
        return visitaNavioServico.planejarAtracacao(identificador, dto);
    }

    @PostMapping("/{identificador}/chegada")
    public VisitaNavioDetalheDTO confirmarChegada(@PathVariable Long identificador) {
        return visitaNavioServico.registrarChegada(identificador);
    }

    @PostMapping("/{identificador}/atracar")
    public VisitaNavioDetalheDTO atracar(@PathVariable Long identificador) {
        return visitaNavioServico.registrarAtracacao(identificador);
    }

    @PostMapping("/{identificador}/iniciar-operacao")
    public VisitaNavioDetalheDTO iniciarOperacao(@PathVariable Long identificador) {
        return visitaNavioServico.iniciarOperacao(identificador);
    }

    @PostMapping("/{identificador}/concluir-operacao")
    public VisitaNavioDetalheDTO concluirOperacao(@PathVariable Long identificador) {
        return visitaNavioServico.concluirOperacao(identificador);
    }

    @PostMapping("/{identificador}/desatracar")
    public VisitaNavioDetalheDTO desatracar(@PathVariable Long identificador) {
        return visitaNavioServico.registrarDesatracacao(identificador);
    }

    @PostMapping("/{identificador}/cancelar")
    public VisitaNavioDetalheDTO cancelar(@PathVariable Long identificador) {
        return visitaNavioServico.cancelar(identificador);
    }

    @GetMapping("/{identificador}/operacoes")
    public List<OperacaoNavioConteinerDTO> listarOperacoes(@PathVariable Long identificador) {
        return visitaNavioServico.listarOperacoes(identificador);
    }

    @PostMapping("/{identificador}/operacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public OperacaoNavioConteinerDTO adicionarOperacao(@PathVariable Long identificador,
                                                       @Valid @RequestBody OperacaoNavioConteinerRequest request) {
        return visitaNavioServico.adicionarOperacao(identificador, request);
    }

    @PatchMapping("/{identificador}/operacoes/{operacaoId}/status")
    public OperacaoNavioConteinerDTO atualizarStatusOperacao(@PathVariable Long identificador,
                                                             @PathVariable Long operacaoId,
                                                             @Valid @RequestBody AtualizacaoStatusOperacaoDTO dto) {
        return visitaNavioServico.atualizarStatusOperacao(identificador, operacaoId, dto);
    }

    @DeleteMapping("/{identificador}/operacoes/{operacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerOperacao(@PathVariable Long identificador, @PathVariable Long operacaoId) {
        visitaNavioServico.removerOperacao(identificador, operacaoId);
    }
}
