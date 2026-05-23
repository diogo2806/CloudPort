package br.com.cloudport.servicoyard.dispatch.controlador;

import br.com.cloudport.servicoyard.dispatch.dto.CadastroInstrucaoMovimentacaoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.DespachoInstrucaoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.EquipamentoResumoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.InstrucaoMovimentacaoDTO;
import br.com.cloudport.servicoyard.dispatch.servico.InstrucaoMovimentacaoServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/dispatch")
@Validated
public class DispatchControlador {

    private final InstrucaoMovimentacaoServico instrucaoServico;

    public DispatchControlador(InstrucaoMovimentacaoServico instrucaoServico) {
        this.instrucaoServico = instrucaoServico;
    }

    @GetMapping("/instrucoes")
    public List<InstrucaoMovimentacaoDTO> listar() {
        return instrucaoServico.listar();
    }

    @GetMapping("/instrucoes/{id}")
    public InstrucaoMovimentacaoDTO detalhar(@PathVariable Long id) {
        return instrucaoServico.buscar(id);
    }

    @PostMapping("/instrucoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InstrucaoMovimentacaoDTO planejar(@Valid @RequestBody CadastroInstrucaoMovimentacaoDTO dto) {
        return instrucaoServico.planejar(dto);
    }

    @PostMapping("/instrucoes/{id}/despacho")
    public InstrucaoMovimentacaoDTO despachar(@PathVariable Long id, @Valid @RequestBody DespachoInstrucaoDTO dto) {
        return instrucaoServico.despachar(id, dto);
    }

    @PostMapping("/instrucoes/{id}/iniciar")
    public InstrucaoMovimentacaoDTO iniciar(@PathVariable Long id) {
        return instrucaoServico.iniciar(id);
    }

    @PostMapping("/instrucoes/{id}/concluir")
    public InstrucaoMovimentacaoDTO concluir(@PathVariable Long id) {
        return instrucaoServico.concluir(id);
    }

    @PostMapping("/instrucoes/{id}/cancelar")
    public InstrucaoMovimentacaoDTO cancelar(@PathVariable Long id) {
        return instrucaoServico.cancelar(id);
    }

    @GetMapping("/equipamentos")
    public List<EquipamentoResumoDTO> listarEquipamentos() {
        return instrucaoServico.listarEquipamentos();
    }

    @GetMapping("/equipamentos/{equipamentoId}/job-list")
    public List<InstrucaoMovimentacaoDTO> jobList(@PathVariable Long equipamentoId) {
        return instrucaoServico.jobListEquipamento(equipamentoId);
    }
}
