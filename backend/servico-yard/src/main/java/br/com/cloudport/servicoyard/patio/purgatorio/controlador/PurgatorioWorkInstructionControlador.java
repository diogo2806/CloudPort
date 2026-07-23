package br.com.cloudport.servicoyard.patio.purgatorio.controlador;

import br.com.cloudport.servicoyard.patio.purgatorio.dto.ComandoPurgatorioWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.CasoPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.servico.PurgatorioWorkInstructionServico;
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
@RequestMapping("/yard/patio/purgatorio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class PurgatorioWorkInstructionControlador {

    private final PurgatorioWorkInstructionServico servico;

    public PurgatorioWorkInstructionControlador(PurgatorioWorkInstructionServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public CasoPurgatorioWorkInstruction abrir(@Valid @RequestBody ComandoPurgatorioWorkInstructionDto dto) {
        return servico.abrir(dto);
    }

    @GetMapping
    public List<CasoPurgatorioWorkInstruction> listar(
            @RequestParam(name = "workQueueId", required = false) Long workQueueId) {
        return workQueueId == null ? servico.listarAbertos() : servico.listarPorFila(workQueueId);
    }

    @PostMapping("/{id}/corrigir")
    public CasoPurgatorioWorkInstruction corrigir(@PathVariable Long id,
                                                   @Valid @RequestBody ComandoPurgatorioWorkInstructionDto dto) {
        return servico.corrigir(id, dto);
    }

    @PostMapping("/{id}/substituir")
    public CasoPurgatorioWorkInstruction substituir(@PathVariable Long id,
                                                     @Valid @RequestBody ComandoPurgatorioWorkInstructionDto dto) {
        return servico.substituir(id, dto);
    }

    @PostMapping("/{id}/reencaminhar")
    public CasoPurgatorioWorkInstruction reencaminhar(@PathVariable Long id,
                                                       @Valid @RequestBody ComandoPurgatorioWorkInstructionDto dto) {
        return servico.reencaminhar(id, dto);
    }

    @PostMapping("/{id}/revalidar")
    public CasoPurgatorioWorkInstruction revalidar(@PathVariable Long id,
                                                    @Valid @RequestBody ComandoPurgatorioWorkInstructionDto dto) {
        return servico.revalidar(id, dto);
    }

    @PostMapping("/{id}/cancelar")
    public CasoPurgatorioWorkInstruction cancelar(@PathVariable Long id,
                                                   @Valid @RequestBody ComandoPurgatorioWorkInstructionDto dto) {
        return servico.cancelar(id, dto);
    }
}
