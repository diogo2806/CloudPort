package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.dto.AgendamentoRequest;
import br.com.cloudport.servicogate.dto.DocumentoAgendamentoDTO;
import br.com.cloudport.servicogate.dto.DocumentoUploadRequest;
import br.com.cloudport.servicogate.service.AgendamentoService;
import br.com.cloudport.servicogate.service.DocumentoDownload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/gate/agendamentos")
@Validated
@Tag(name = "Agendamentos", description = "Gestão de agendamentos de gate")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos com filtros de período e paginação")
    public Page<AgendamentoDTO> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                       Pageable pageable) {
        return agendamentoService.buscar(dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um agendamento específico")
    public AgendamentoDTO buscarPorId(@PathVariable Long id) {
        return agendamentoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria um novo agendamento")
    public ResponseEntity<AgendamentoDTO> criar(@Valid @RequestBody AgendamentoRequest request) {
        AgendamentoDTO criado = agendamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento existente")
    public AgendamentoDTO atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoRequest request) {
        return agendamentoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela um agendamento")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Realiza upload de documento para o agendamento")
    public ResponseEntity<DocumentoAgendamentoDTO> uploadDocumento(@PathVariable Long id,
                                                                   @Valid @RequestPart("metadata") DocumentoUploadRequest metadata,
                                                                   @RequestPart("file") MultipartFile arquivo) {
        DocumentoAgendamentoDTO documento = agendamentoService.adicionarDocumento(id, metadata, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(documento);
    }

    @GetMapping("/{id}/documentos")
    @Operation(summary = "Lista documentos de um agendamento")
    public List<DocumentoAgendamentoDTO> listarDocumentos(@PathVariable Long id) {
        return agendamentoService.listarDocumentos(id);
    }

    @GetMapping("/{agendamentoId}/documentos/{documentoId}")
    @Operation(summary = "Download de documento do agendamento")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocumento(@PathVariable Long agendamentoId,
                                                                                  @PathVariable Long documentoId) {
        DocumentoDownload download = agendamentoService.baixarDocumento(agendamentoId, documentoId);
        ContentDisposition disposition = ContentDisposition.attachment().filename(download.getFilename()).build();
        return ResponseEntity.ok()
                .header("Content-Disposition", disposition.toString())
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .body(download.getResource());
    }
}
