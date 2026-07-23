package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.dto.TipoIsoDTO;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.servico.TipoIsoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/yard/tipos-iso")
@Tag(name = "Tipos ISO", description = "Manutenção mestre dos tipos ISO de equipamentos")
public class TipoIsoControlador {
    private final TipoIsoServico servico;

    public TipoIsoControlador(TipoIsoServico servico) { this.servico = servico; }

    @GetMapping
    @Operation(summary = "Listar tipos ISO com filtros")
    public List<TipoIsoDTO.Resposta> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Long grupoIsoId,
            @RequestParam(required = false) TipoEquipamentoInventario.CategoriaEquipamento categoria,
            @RequestParam(required = false) Boolean refrigerado,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean arquetipo) {
        return servico.listar(termo, grupoIsoId, categoria, refrigerado, ativo, arquetipo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar tipo ISO e dependências")
    public TipoIsoDTO.Resposta detalhar(@PathVariable Long id) { return servico.detalhar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar tipo ISO")
    public TipoIsoDTO.Resposta criar(@RequestBody TipoIsoDTO.ManutencaoRequest request) { return servico.criar(request); }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tipo ISO preservando o ISO ID")
    public TipoIsoDTO.Resposta atualizar(@PathVariable Long id, @RequestBody TipoIsoDTO.ManutencaoRequest request) {
        return servico.atualizar(id, request);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(summary = "Inativar ou reativar tipo ISO")
    public TipoIsoDTO.Resposta alterarSituacao(@PathVariable Long id, @RequestBody TipoIsoDTO.SituacaoRequest request) {
        return servico.alterarSituacao(id, request);
    }

    @GetMapping("/{id}/dependencias")
    @Operation(summary = "Consultar dependências e motivo de bloqueio")
    public TipoIsoDTO.Resposta dependencias(@PathVariable Long id) { return servico.detalhar(id); }
}