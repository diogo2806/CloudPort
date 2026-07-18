package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoStatusOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AuditoriaComandoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/ordens")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','SERVICE_NAVIO')")
public class CancelamentoOrdemTrabalhoPatioControlador {

    private final OrdemTrabalhoPatioServico ordemServico;
    private final AuditoriaComandoPatioServico auditoriaServico;

    public CancelamentoOrdemTrabalhoPatioControlador(
            OrdemTrabalhoPatioServico ordemServico,
            AuditoriaComandoPatioServico auditoriaServico) {
        this.ordemServico = ordemServico;
        this.auditoriaServico = auditoriaServico;
    }

    @PatchMapping("/{id}/cancelar")
    public OrdemTrabalhoPatioRespostaDto cancelar(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivadoDto comando) {
        AtualizacaoStatusOrdemTrabalhoDto atualizacao = new AtualizacaoStatusOrdemTrabalhoDto();
        atualizacao.setStatusOrdem(StatusOrdemTrabalhoPatio.CANCELADA);
        atualizacao.setMotivo(comando.getMotivo());
        atualizacao.setUsuario(comando.getUsuario());
        atualizacao.setOrigemAcao(comando.getOrigemAcao());
        atualizacao.setCorrelationId(comando.getCorrelationId());

        OrdemTrabalhoPatioRespostaDto resposta = ordemServico.atualizarStatus(id, atualizacao);
        auditoriaServico.registrar(
                null,
                id,
                "ORDEM_CANCELADA_COM_MOTIVO",
                comando,
                "Ordem cancelada administrativamente.");
        return resposta;
    }
}
