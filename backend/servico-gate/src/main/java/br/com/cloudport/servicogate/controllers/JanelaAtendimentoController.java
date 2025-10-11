package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.dto.JanelaAtendimentoDTO;
import br.com.cloudport.servicogate.dto.JanelaAtendimentoRequest;
import br.com.cloudport.servicogate.service.JanelaAtendimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import javax.validation.Valid;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/janelas")
@Validated
@Tag(name = "Janelas de Atendimento", description = "Gerenciamento de janelas de atendimento")
public class JanelaAtendimentoController {

    private final JanelaAtendimentoService janelaAtendimentoService;

    public JanelaAtendimentoController(JanelaAtendimentoService janelaAtendimentoService) {
        this.janelaAtendimentoService = janelaAtendimentoService;
    }

    @GetMapping
    @Operation(summary = "Lista janelas de atendimento com filtros de período")
    public Page<JanelaAtendimentoDTO> listar(@Parameter(description = "Data inicial do período (inclusive)")
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                             @Parameter(description = "Data final do período (inclusive)")
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                             @ParameterObject Pageable pageable) {
        return janelaAtendimentoService.buscar(dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém detalhes de uma janela de atendimento")
    public JanelaAtendimentoDTO buscarPorId(@PathVariable Long id) {
        return janelaAtendimentoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma nova janela de atendimento")
    public ResponseEntity<JanelaAtendimentoDTO> criar(@Valid @RequestBody JanelaAtendimentoRequest request) {
        JanelaAtendimentoDTO criado = janelaAtendimentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma janela de atendimento")
    public JanelaAtendimentoDTO atualizar(@PathVariable Long id, @Valid @RequestBody JanelaAtendimentoRequest request) {
        return janelaAtendimentoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma janela de atendimento")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        janelaAtendimentoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
