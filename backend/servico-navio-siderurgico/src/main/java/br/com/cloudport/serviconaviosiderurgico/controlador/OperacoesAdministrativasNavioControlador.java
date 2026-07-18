package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.contracts.api.ComandoMotivado;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.OperacoesAdministrativasNavioServico;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visitas-navio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
public class OperacoesAdministrativasNavioControlador {

    private final OperacoesAdministrativasNavioServico servico;

    public OperacoesAdministrativasNavioControlador(OperacoesAdministrativasNavioServico servico) {
        this.servico = servico;
    }

    @PatchMapping("/{id}/cancelar")
    public VisitaNavioDTO cancelarVisita(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivado comando) {
        return servico.cancelarVisita(id, comando);
    }

    @PatchMapping("/{id}/itens/{itemId}/cancelar")
    public ItemOperacaoNavioDTO cancelarItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody ComandoMotivado comando) {
        return servico.cancelarItem(id, itemId, comando);
    }

    @PostMapping("/{id}/plano-estiva/{planoId}/publicar")
    public PlanoEstivaNavioDTO publicarPlano(
            @PathVariable Long id,
            @PathVariable Long planoId,
            @Valid @RequestBody ComandoMotivado comando) {
        return servico.publicarPlano(id, planoId, comando);
    }

    @PatchMapping("/{id}/plano-estiva/{planoId}/invalidar")
    public PlanoEstivaNavioDTO invalidarPlano(
            @PathVariable Long id,
            @PathVariable Long planoId,
            @Valid @RequestBody ComandoMotivado comando) {
        return servico.invalidarPlano(id, planoId, comando);
    }

    @PatchMapping("/{id}/plano-estiva/{planoId}/cancelar")
    public PlanoEstivaNavioDTO cancelarPlano(
            @PathVariable Long id,
            @PathVariable Long planoId,
            @Valid @RequestBody ComandoMotivado comando) {
        return servico.cancelarPlano(id, planoId, comando);
    }

    @PatchMapping("/{id}/integracao-patio/ordens/{ordemId}/cancelar")
    public OrdemPatioDaVisitaDTO cancelarOrdem(
            @PathVariable Long id,
            @PathVariable Long ordemId,
            @Valid @RequestBody ComandoMotivado comando) {
        return servico.cancelarOrdem(id, ordemId, comando);
    }
}
