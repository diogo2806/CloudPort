package br.com.cloudport.servicorail.ferrovia.locomotiva.controlador;

import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.CadastroTransferenciaLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfirmacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.EntregaCustodiaLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.LiberacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.PlanejamentoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.TransferenciaLocomotivaRespostaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.servico.TransferenciaLocomotivaServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia/locomotivas-transferencia")
public class TransferenciaLocomotivaControlador {

    private final TransferenciaLocomotivaServico transferenciaLocomotivaServico;

    public TransferenciaLocomotivaControlador(TransferenciaLocomotivaServico transferenciaLocomotivaServico) {
        this.transferenciaLocomotivaServico = transferenciaLocomotivaServico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferenciaLocomotivaRespostaDto cadastrar(
            @Valid @RequestBody CadastroTransferenciaLocomotivaDto dto) {
        return transferenciaLocomotivaServico.cadastrar(dto);
    }

    @GetMapping
    public List<TransferenciaLocomotivaRespostaDto> listar() {
        return transferenciaLocomotivaServico.listar();
    }

    @GetMapping("/{id}")
    public TransferenciaLocomotivaRespostaDto consultar(@PathVariable("id") Long id) {
        return transferenciaLocomotivaServico.consultar(id);
    }

    @PostMapping("/{id}/entrega-custodia")
    public TransferenciaLocomotivaRespostaDto registrarEntregaCustodia(
            @PathVariable("id") Long id,
            @Valid @RequestBody EntregaCustodiaLocomotivaDto dto) {
        return transferenciaLocomotivaServico.registrarEntregaCustodia(id, dto);
    }

    @PostMapping("/{id}/planejamento-embarque")
    public TransferenciaLocomotivaRespostaDto planejarEmbarque(
            @PathVariable("id") Long id,
            @Valid @RequestBody PlanejamentoEmbarqueLocomotivaDto dto) {
        return transferenciaLocomotivaServico.planejarEmbarque(id, dto);
    }

    @PostMapping("/{id}/liberacao-embarque")
    public TransferenciaLocomotivaRespostaDto liberarEmbarque(
            @PathVariable("id") Long id,
            @Valid @RequestBody LiberacaoEmbarqueLocomotivaDto dto) {
        return transferenciaLocomotivaServico.liberarEmbarque(id, dto);
    }

    @PostMapping("/{id}/confirmacao-embarque")
    public TransferenciaLocomotivaRespostaDto confirmarEmbarque(
            @PathVariable("id") Long id,
            @Valid @RequestBody ConfirmacaoEmbarqueLocomotivaDto dto) {
        return transferenciaLocomotivaServico.confirmarEmbarque(id, dto);
    }
}
