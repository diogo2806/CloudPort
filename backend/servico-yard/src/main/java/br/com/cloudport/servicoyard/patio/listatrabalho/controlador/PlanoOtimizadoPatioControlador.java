package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.CompensacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.PlanoOtimizadoPatioServico;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/planos-otimizados")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','SERVICE_NAVIO')")
public class PlanoOtimizadoPatioControlador {

    private final PlanoOtimizadoPatioServico planoOtimizadoPatioServico;

    public PlanoOtimizadoPatioControlador(PlanoOtimizadoPatioServico planoOtimizadoPatioServico) {
        this.planoOtimizadoPatioServico = planoOtimizadoPatioServico;
    }

    @PostMapping("/aplicar")
    public ResultadoAplicacaoPlanoOtimizadoPatioDto aplicar(
            @Valid @RequestBody AplicacaoPlanoOtimizadoPatioDto comando
    ) {
        return planoOtimizadoPatioServico.aplicar(comando);
    }

    @PostMapping("/compensar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void compensar(@Valid @RequestBody CompensacaoPlanoOtimizadoPatioDto comando) {
        planoOtimizadoPatioServico.compensar(comando);
    }
}
