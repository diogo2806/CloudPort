package br.com.cloudport.servicoyard.container.controlador;

import br.com.cloudport.servicoyard.container.dto.InventarioConteinerRespostaDTO;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.container.servico.InventarioConteinerServico;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/inventario")
@Tag(name = "Inventário do Pátio", description = "Consulta consolidada do inventário operacional de contêineres")
public class InventarioConteinerControlador {

    private final InventarioConteinerServico inventarioConteinerServico;

    public InventarioConteinerControlador(InventarioConteinerServico inventarioConteinerServico) {
        this.inventarioConteinerServico = inventarioConteinerServico;
    }

    @GetMapping
    @Operation(summary = "Consultar inventário operacional de contêineres")
    public InventarioConteinerRespostaDTO consultar(
            @RequestParam(value = "codigo", required = false) String codigo,
            @RequestParam(value = "status", required = false) StatusConteiner status,
            @RequestParam(value = "tipoCarga", required = false) TipoCargaConteiner tipoCarga) {
        return inventarioConteinerServico.consultar(codigo, status, tipoCarga);
    }
}
