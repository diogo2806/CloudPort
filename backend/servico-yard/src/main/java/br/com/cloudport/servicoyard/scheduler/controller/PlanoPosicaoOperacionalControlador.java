package br.com.cloudport.servicoyard.scheduler.controller;

import br.com.cloudport.servicoyard.scheduler.dto.AlteracaoEstadoPlanoPosicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.HistoricoPlanoPosicaoOperacionalDto;
import br.com.cloudport.servicoyard.scheduler.dto.PlanoPosicaoOperacionalRespostaDto;
import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.servico.PlanoPosicaoOperacionalServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler/planos-posicao")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class PlanoPosicaoOperacionalControlador {

    private final PlanoPosicaoOperacionalServico servico;

    public PlanoPosicaoOperacionalControlador(PlanoPosicaoOperacionalServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<PlanoPosicaoOperacionalRespostaDto> listar(
            @RequestParam(name = "estado", required = false) EstadoPlanoPosicaoOperacional estado,
            @RequestParam(name = "bloco", required = false) String bloco) {
        return servico.listar(estado, bloco);
    }

    @GetMapping("/{planoId}/historico")
    public List<HistoricoPlanoPosicaoOperacionalDto> listarHistorico(
            @PathVariable("planoId") Long planoId) {
        return servico.listarHistorico(planoId);
    }

    @PostMapping("/{planoId}/estado")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public PlanoPosicaoOperacionalRespostaDto alterarEstado(
            @PathVariable("planoId") Long planoId,
            @Valid @RequestBody AlteracaoEstadoPlanoPosicaoDto comando) {
        return servico.alterarEstado(planoId, comando);
    }
}
