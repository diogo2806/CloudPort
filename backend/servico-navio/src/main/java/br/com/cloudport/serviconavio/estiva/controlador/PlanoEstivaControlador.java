package br.com.cloudport.serviconavio.estiva.controlador;

import br.com.cloudport.serviconavio.estiva.dto.CriarAtribuicaoEstivaDTO;
import br.com.cloudport.serviconavio.estiva.dto.CriarPlanoEstivaDTO;
import br.com.cloudport.serviconavio.estiva.dto.EmbarqueDiretoGateDTO;
import br.com.cloudport.serviconavio.estiva.dto.EmbarqueDiretoGateResultadoDTO;
import br.com.cloudport.serviconavio.estiva.dto.PlanoEstivaDetalheDTO;
import br.com.cloudport.serviconavio.estiva.servico.PlanoEstivaServico;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class PlanoEstivaControlador {

    private final PlanoEstivaServico planoEstivaServico;

    public PlanoEstivaControlador(PlanoEstivaServico planoEstivaServico) {
        this.planoEstivaServico = planoEstivaServico;
    }

    @GetMapping("/escalas/{escalaId}/plano-estiva")
    public PlanoEstivaDetalheDTO buscar(@PathVariable Long escalaId) {
        return planoEstivaServico.buscarPorEscala(escalaId);
    }

    @PostMapping("/escalas/{escalaId}/plano-estiva")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoEstivaDetalheDTO criar(@PathVariable Long escalaId,
                                       @Valid @RequestBody CriarPlanoEstivaDTO dto) {
        return planoEstivaServico.criar(escalaId, dto);
    }

    @PostMapping("/escalas/{escalaId}/plano-estiva/atribuicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoEstivaDetalheDTO adicionarAtribuicao(@PathVariable Long escalaId,
                                                     @Valid @RequestBody CriarAtribuicaoEstivaDTO dto) {
        return planoEstivaServico.adicionarAtribuicao(escalaId, dto);
    }

    @PatchMapping("/plano-estiva/atribuicoes/{atribuicaoId}/embarcar")
    public PlanoEstivaDetalheDTO embarcar(@PathVariable Long atribuicaoId) {
        return planoEstivaServico.embarcar(atribuicaoId);
    }

    @PatchMapping("/plano-estiva/atribuicoes/{atribuicaoId}/embarcar-direto-gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public EmbarqueDiretoGateResultadoDTO embarcarDiretoDoGate(
            @PathVariable Long atribuicaoId,
            @Valid @RequestBody EmbarqueDiretoGateDTO dto) {
        return planoEstivaServico.embarcarDiretoDoGate(
                atribuicaoId,
                dto.getCodigoConteiner(),
                dto.getEmbarcadoEm());
    }

    @DeleteMapping("/plano-estiva/atribuicoes/{atribuicaoId}")
    public PlanoEstivaDetalheDTO removerAtribuicao(@PathVariable Long atribuicaoId) {
        return planoEstivaServico.removerAtribuicao(atribuicaoId);
    }
}
