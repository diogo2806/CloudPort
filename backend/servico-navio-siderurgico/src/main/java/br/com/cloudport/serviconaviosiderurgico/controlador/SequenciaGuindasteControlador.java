package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusSequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandosSequenciaGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.SequenciaGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.SequenciaGuindasteServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crane-sequences")
@Tag(name = "Crane Sequences", description = "Execucao e reconciliacao de movimentos dos guindastes de navio")
public class SequenciaGuindasteControlador {

    private final SequenciaGuindasteServico sequenciaServico;

    public SequenciaGuindasteControlador(SequenciaGuindasteServico sequenciaServico) {
        this.sequenciaServico = sequenciaServico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar ou obter uma sequencia pelo movementId")
    public SequenciaGuindasteDTO criar(@Valid @RequestBody ComandosSequenciaGuindasteDTO.Criar comando) {
        return sequenciaServico.criarOuObter(comando);
    }

    @PostMapping("/{movementId}/start")
    @Operation(summary = "Iniciar ou retomar um movimento")
    public SequenciaGuindasteDTO iniciar(
            @PathVariable String movementId,
            @Valid @RequestBody ComandosSequenciaGuindasteDTO.Transicao comando
    ) {
        return sequenciaServico.iniciar(movementId, comando);
    }

    @PostMapping("/{movementId}/pause")
    @Operation(summary = "Pausar um movimento iniciado")
    public SequenciaGuindasteDTO pausar(
            @PathVariable String movementId,
            @Valid @RequestBody ComandosSequenciaGuindasteDTO.Transicao comando
    ) {
        return sequenciaServico.pausar(movementId, comando);
    }

    @PostMapping("/{movementId}/finish")
    @Operation(summary = "Finalizar um movimento iniciado")
    public SequenciaGuindasteDTO finalizar(
            @PathVariable String movementId,
            @Valid @RequestBody ComandosSequenciaGuindasteDTO.Transicao comando
    ) {
        return sequenciaServico.finalizar(movementId, comando);
    }

    @PostMapping("/{movementId}/cancel")
    @Operation(summary = "Cancelar um movimento ainda nao finalizado")
    public SequenciaGuindasteDTO cancelar(
            @PathVariable String movementId,
            @Valid @RequestBody ComandosSequenciaGuindasteDTO.Transicao comando
    ) {
        return sequenciaServico.cancelar(movementId, comando);
    }

    @GetMapping("/{movementId}")
    @Operation(summary = "Consultar uma sequencia pelo movementId")
    public SequenciaGuindasteDTO buscar(@PathVariable String movementId) {
        return sequenciaServico.buscar(movementId);
    }

    @GetMapping("/{movementId}/history")
    @Operation(summary = "Consultar a trilha de auditoria do movimento")
    public List<SequenciaGuindasteDTO.Auditoria> listarAuditoria(@PathVariable String movementId) {
        return sequenciaServico.listarAuditoria(movementId);
    }

    @GetMapping
    @Operation(summary = "Listar sequencias por visita, estado e janela planejada")
    public List<SequenciaGuindasteDTO> listar(
            @RequestParam(required = false) String vesselVisitId,
            @RequestParam(required = false) StatusSequenciaGuindaste status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return sequenciaServico.listar(vesselVisitId, status, from, to);
    }
}
