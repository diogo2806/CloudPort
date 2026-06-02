package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.KpiPatioDto;
import br.com.cloudport.servicoyard.patio.servico.KpiPatioServico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard/patio/kpi")
public class KpiPatioControlador {

    private final KpiPatioServico kpiPatioServico;

    public KpiPatioControlador(KpiPatioServico kpiPatioServico) {
        this.kpiPatioServico = kpiPatioServico;
    }

    @GetMapping("/calcular")
    public ResponseEntity<KpiPatioDto> calcularKpis() {
        var kpis = kpiPatioServico.calcularKpis();
        return ResponseEntity.ok(kpis);
    }
}
