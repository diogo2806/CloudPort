package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.ReeferTelemetriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.ReeferTelemetriaPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.servico.ReeferTelemetriaPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/reefers/telemetria")
public class ReeferTelemetriaPatioControlador {

    private final ReeferTelemetriaPatioServico servico;

    public ReeferTelemetriaPatioControlador(ReeferTelemetriaPatioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ReeferTelemetriaPatioDto> listar() {
        return servico.listar();
    }

    @PutMapping("/{conteinerId}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','SERVICE_NAVIO')")
    public ReeferTelemetriaPatioDto registrar(@PathVariable Long conteinerId,
                                               @Valid @RequestBody ReeferTelemetriaPatioRequisicaoDto dto) {
        return servico.registrar(conteinerId, dto);
    }
}