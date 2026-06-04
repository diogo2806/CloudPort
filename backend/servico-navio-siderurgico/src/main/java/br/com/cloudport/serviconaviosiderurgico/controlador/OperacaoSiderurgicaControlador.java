package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dto.ItemCargaSiderurgicaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OperacaoSiderurgicaDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.ItemCargaSiderurgicaServico;
import br.com.cloudport.serviconaviosiderurgico.servico.OperacaoSiderurgicaServico;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operacoes-siderurgicas")
public class OperacaoSiderurgicaControlador {

    private final OperacaoSiderurgicaServico operacaoServico;
    private final ItemCargaSiderurgicaServico itemServico;

    public OperacaoSiderurgicaControlador(OperacaoSiderurgicaServico operacaoServico, ItemCargaSiderurgicaServico itemServico) {
        this.operacaoServico = operacaoServico;
        this.itemServico = itemServico;
    }

    @GetMapping
    public List<OperacaoSiderurgicaDTO> listar(@RequestParam(required = false) Long navioId) {
        return operacaoServico.listar(navioId);
    }

    @PostMapping
    public ResponseEntity<OperacaoSiderurgicaDTO> criar(@Valid @RequestBody OperacaoSiderurgicaDTO dto) {
        OperacaoSiderurgicaDTO criada = operacaoServico.criar(dto);
        return ResponseEntity.created(URI.create("/operacoes-siderurgicas/" + criada.id())).body(criada);
    }

    @GetMapping("/{operacaoId}/itens")
    public List<ItemCargaSiderurgicaDTO> listarItens(@PathVariable Long operacaoId) {
        return itemServico.listar(operacaoId);
    }

    @PostMapping("/{operacaoId}/itens")
    public ResponseEntity<ItemCargaSiderurgicaDTO> criarItem(@PathVariable Long operacaoId,
                                                             @Valid @RequestBody ItemCargaSiderurgicaDTO dto) {
        ItemCargaSiderurgicaDTO criado = itemServico.criar(operacaoId, dto);
        return ResponseEntity.created(URI.create("/operacoes-siderurgicas/" + operacaoId + "/itens/" + criado.id())).body(criado);
    }
}
