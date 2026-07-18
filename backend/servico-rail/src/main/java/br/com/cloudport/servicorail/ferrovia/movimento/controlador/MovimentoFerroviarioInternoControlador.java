package br.com.cloudport.servicorail.ferrovia.movimento.controlador;

import br.com.cloudport.servicorail.ferrovia.movimento.dto.CancelarMovimentoFerroviarioInternoDto;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.MovimentoFerroviarioInternoRespostaDto;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.PlanejarMovimentoFerroviarioInternoDto;
import br.com.cloudport.servicorail.ferrovia.movimento.servico.MovimentoFerroviarioInternoServico;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia/movimentos-internos")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
public class MovimentoFerroviarioInternoControlador {

    private final MovimentoFerroviarioInternoServico servico;

    public MovimentoFerroviarioInternoControlador(MovimentoFerroviarioInternoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public ResponseEntity<MovimentoFerroviarioInternoRespostaDto> planejar(
            @Valid @RequestBody PlanejarMovimentoFerroviarioInternoDto dto,
            Principal principal) {
        MovimentoFerroviarioInternoRespostaDto criado = servico.planejar(dto, usuario(principal));
        return ResponseEntity
                .created(URI.create("/rail/ferrovia/movimentos-internos/" + criado.getId()))
                .body(criado);
    }

    @GetMapping("/{id}")
    public MovimentoFerroviarioInternoRespostaDto consultar(@PathVariable("id") Long id) {
        return servico.consultar(id);
    }

    @GetMapping("/visita/{visitaTremId}")
    public List<MovimentoFerroviarioInternoRespostaDto> listarPorVisita(
            @PathVariable("visitaTremId") Long visitaTremId) {
        return servico.listarPorVisita(visitaTremId);
    }

    @PostMapping("/{id}/autorizar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public MovimentoFerroviarioInternoRespostaDto autorizar(
            @PathVariable("id") Long id,
            Principal principal) {
        return servico.autorizar(id, usuario(principal));
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO')")
    public MovimentoFerroviarioInternoRespostaDto iniciar(
            @PathVariable("id") Long id,
            Principal principal) {
        return servico.iniciar(id, usuario(principal));
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO')")
    public MovimentoFerroviarioInternoRespostaDto concluir(
            @PathVariable("id") Long id,
            Principal principal) {
        return servico.concluir(id, usuario(principal));
    }

    @PostMapping("/{id}/cancelar")
    public MovimentoFerroviarioInternoRespostaDto cancelar(
            @PathVariable("id") Long id,
            @Valid @RequestBody CancelarMovimentoFerroviarioInternoDto dto,
            Principal principal) {
        return servico.cancelar(id, dto, usuario(principal));
    }

    private String usuario(Principal principal) {
        return principal == null ? "SISTEMA" : principal.getName();
    }
}
