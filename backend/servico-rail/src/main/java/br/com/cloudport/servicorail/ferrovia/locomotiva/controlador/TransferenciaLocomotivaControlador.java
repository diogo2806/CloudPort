package br.com.cloudport.servicorail.ferrovia.locomotiva.controlador;

import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfiguracaoLocomotivaVisitaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfirmacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.EntregaCustodiaLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.LiberacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.PlanejamentoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.TransferenciaLocomotivaRespostaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.servico.TransferenciaLocomotivaServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class TransferenciaLocomotivaControlador {

    private final TransferenciaLocomotivaServico transferenciaLocomotivaServico;

    public TransferenciaLocomotivaControlador(TransferenciaLocomotivaServico transferenciaLocomotivaServico) {
        this.transferenciaLocomotivaServico = transferenciaLocomotivaServico;
    }

    @PostMapping("/visitas/{visitaTremId}/locomotiva")
    public TransferenciaLocomotivaRespostaDto configurarVisitaComoLocomotiva(
            @PathVariable("visitaTremId") Long visitaTremId,
            @Valid @RequestBody ConfiguracaoLocomotivaVisitaDto dto) {
        return transferenciaLocomotivaServico.configurarVisitaComoLocomotiva(visitaTremId, dto);
    }

    @GetMapping("/locomotivas")
    public List<TransferenciaLocomotivaRespostaDto> listar() {
        return transferenciaLocomotivaServico.listar();
    }

    @GetMapping("/visitas/{visitaTremId}/locomotiva")
    public TransferenciaLocomotivaRespostaDto consultar(
            @PathVariable("visitaTremId") Long visitaTremId) {
        return transferenciaLocomotivaServico.consultar(visitaTremId);
    }

    @PostMapping("/visitas/{visitaTremId}/locomotiva/entrega-custodia")
    public TransferenciaLocomotivaRespostaDto registrarEntregaCustodia(
            @PathVariable("visitaTremId") Long visitaTremId,
            @Valid @RequestBody EntregaCustodiaLocomotivaDto dto) {
        return transferenciaLocomotivaServico.registrarEntregaCustodia(visitaTremId, dto);
    }

    @PostMapping("/visitas/{visitaTremId}/locomotiva/planejamento-embarque")
    public TransferenciaLocomotivaRespostaDto planejarEmbarque(
            @PathVariable("visitaTremId") Long visitaTremId,
            @Valid @RequestBody PlanejamentoEmbarqueLocomotivaDto dto) {
        return transferenciaLocomotivaServico.planejarEmbarque(visitaTremId, dto);
    }

    @PostMapping("/visitas/{visitaTremId}/locomotiva/liberacao-embarque")
    public TransferenciaLocomotivaRespostaDto liberarEmbarque(
            @PathVariable("visitaTremId") Long visitaTremId,
            @Valid @RequestBody LiberacaoEmbarqueLocomotivaDto dto) {
        return transferenciaLocomotivaServico.liberarEmbarque(visitaTremId, dto);
    }

    @PostMapping("/visitas/{visitaTremId}/locomotiva/confirmacao-embarque")
    public TransferenciaLocomotivaRespostaDto confirmarEmbarque(
            @PathVariable("visitaTremId") Long visitaTremId,
            @Valid @RequestBody ConfirmacaoEmbarqueLocomotivaDto dto) {
        return transferenciaLocomotivaServico.confirmarEmbarque(visitaTremId, dto);
    }
}
