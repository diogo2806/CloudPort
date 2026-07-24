package br.com.cloudport.servicogate.app.frota;

import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Resposta;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Salvar;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.TransportadoraVinculada;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/frota/veiculos")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR','TRANSPORTADORA')")
public class VeiculoFrotaControlador {

    private final VeiculoFrotaServico servico;

    public VeiculoFrotaControlador(VeiculoFrotaServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Resposta> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Long transportadoraId,
            @RequestParam(required = false) Boolean ativo,
            Authentication authentication) {
        return servico.listar(busca, transportadoraId, ativo, authentication);
    }

    @GetMapping("/minha-transportadora")
    @PreAuthorize("hasRole('TRANSPORTADORA')")
    public TransportadoraVinculada obterMinhaTransportadora(Authentication authentication) {
        return servico.obterMinhaTransportadora(authentication);
    }

    @GetMapping("/elegiveis")
    public List<Resposta> listarElegiveis(
            @RequestParam Long transportadoraId,
            Authentication authentication) {
        return servico.listarElegiveis(transportadoraId, authentication);
    }

    @PostMapping
    public Resposta criar(@Valid @RequestBody Salvar dto, Authentication authentication) {
        return servico.criar(dto, authentication);
    }

    @PutMapping("/{id}")
    public Resposta atualizar(
            @PathVariable Long id,
            @Valid @RequestBody Salvar dto,
            Authentication authentication) {
        return servico.atualizar(id, dto, authentication);
    }

    @PatchMapping("/{id}/status")
    public Resposta alterarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            Authentication authentication) {
        return servico.alterarStatus(id, ativo, authentication);
    }
}
