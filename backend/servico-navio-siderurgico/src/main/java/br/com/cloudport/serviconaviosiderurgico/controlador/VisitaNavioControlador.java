package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.AtualizarFaseVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.AtualizarStatusItemNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.BloqueioItemNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PosicaoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResumoOperacionalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoPlanoEstivaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.ItemOperacaoNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.PlanoEstivaNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/visitas-navio")
public class VisitaNavioControlador {

    private final VisitaNavioServico visitaServico;
    private final ItemOperacaoNavioServico itemServico;
    private final PlanoEstivaNavioServico planoServico;

    public VisitaNavioControlador(VisitaNavioServico visitaServico, ItemOperacaoNavioServico itemServico, PlanoEstivaNavioServico planoServico) {
        this.visitaServico = visitaServico;
        this.itemServico = itemServico;
        this.planoServico = planoServico;
    }

    @GetMapping
    public List<VisitaNavioDTO> listar(
            @RequestParam(required = false) FaseVisitaNavio fase,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Long navioId
    ) {
        return visitaServico.listar(fase, dataInicio, dataFim, navioId);
    }

    @GetMapping("/{id}")
    public VisitaNavioDTO detalhar(@PathVariable Long id) {
        return visitaServico.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaNavioDTO criar(@Valid @RequestBody VisitaNavioDTO dto) {
        return visitaServico.criar(dto);
    }

    @PutMapping("/{id}")
    public VisitaNavioDTO atualizar(@PathVariable Long id, @Valid @RequestBody VisitaNavioDTO dto) {
        return visitaServico.atualizar(id, dto);
    }

    @PatchMapping("/{id}/fase")
    public VisitaNavioDTO alterarFase(@PathVariable Long id, @Valid @RequestBody AtualizarFaseVisitaNavioDTO dto) {
        return visitaServico.alterarFase(id, dto.fase(), dto.usuario(), dto.observacao());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        visitaServico.excluir(id);
    }

    @GetMapping("/{id}/itens")
    public List<ItemOperacaoNavioDTO> listarItens(
            @PathVariable Long id,
            @RequestParam(required = false) TipoMovimentoNavio tipoMovimento,
            @RequestParam(required = false) StatusItemCarga status
    ) {
        return itemServico.listar(id, tipoMovimento, status);
    }

    @PostMapping("/{id}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemOperacaoNavioDTO criarItem(@PathVariable Long id, @Valid @RequestBody ItemOperacaoNavioDTO dto) {
        return itemServico.criar(id, dto);
    }

    @PutMapping("/{id}/itens/{itemId}")
    public ItemOperacaoNavioDTO atualizarItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody ItemOperacaoNavioDTO dto) {
        return itemServico.atualizar(id, itemId, dto);
    }

    @PatchMapping("/{id}/itens/{itemId}/status")
    public ItemOperacaoNavioDTO alterarStatusItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody AtualizarStatusItemNavioDTO dto) {
        return itemServico.alterarStatus(id, itemId, dto.status(), dto.usuario(), dto.observacao());
    }

    @PatchMapping("/{id}/itens/{itemId}/bloqueio")
    public ItemOperacaoNavioDTO alterarBloqueioItem(@PathVariable Long id, @PathVariable Long itemId, @RequestBody BloqueioItemNavioDTO dto) {
        return itemServico.alterarBloqueio(id, itemId, dto);
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirItem(@PathVariable Long id, @PathVariable Long itemId) {
        itemServico.excluir(id, itemId);
    }

    @GetMapping("/{id}/plano-estiva")
    public PlanoEstivaNavioDTO obterPlano(@PathVariable Long id) {
        return planoServico.obter(id);
    }

    @PostMapping("/{id}/plano-estiva")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoEstivaNavioDTO criarPlano(@PathVariable Long id, @RequestBody(required = false) PlanoEstivaNavioDTO dto) {
        return planoServico.criar(id, dto);
    }

    @PutMapping("/{id}/plano-estiva/{planoId}/posicoes")
    public PlanoEstivaNavioDTO atualizarPosicoes(@PathVariable Long id, @PathVariable Long planoId, @Valid @RequestBody List<PosicaoEstivaNavioDTO> posicoes) {
        return planoServico.atualizarPosicoes(id, planoId, posicoes);
    }

    @PostMapping("/{id}/plano-estiva/{planoId}/validar")
    public ValidacaoPlanoEstivaDTO validarPlano(@PathVariable Long id, @PathVariable Long planoId) {
        return planoServico.validar(id, planoId);
    }

    @PostMapping("/{id}/plano-estiva/{planoId}/concluir")
    public PlanoEstivaNavioDTO concluirPlano(@PathVariable Long id, @PathVariable Long planoId) {
        return planoServico.concluir(id, planoId);
    }

    @GetMapping("/{id}/resumo-operacional")
    public ResumoOperacionalNavioDTO resumo(@PathVariable Long id) {
        return visitaServico.resumo(id);
    }

    @GetMapping("/{id}/eventos")
    public List<EventoVisitaNavioDTO> eventos(@PathVariable Long id) {
        return visitaServico.eventos(id);
    }
}
