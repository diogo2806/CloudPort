package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoRequest;
import br.com.cloudport.servicogate.app.cidadao.dto.DocumentoAgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.DocumentoUploadRequest;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoService;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRealtimeService;
import br.com.cloudport.servicogate.app.cidadao.DocumentoDownload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/gate/agendamentos")
@Validated
@Tag(name = "Agendamentos", description = "Gestão de agendamentos de gate")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final AgendamentoRealtimeService agendamentoRealtimeService;

    public AgendamentoController(AgendamentoService agendamentoService,
                                 AgendamentoRealtimeService agendamentoRealtimeService) {
        this.agendamentoService = agendamentoService;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos com filtros de período e paginação")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public Page<AgendamentoDTO> listar(@Parameter(description = "Data inicial do período (inclusive)")
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                       @Parameter(description = "Data final do período (inclusive)")
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                       @ParameterObject Pageable pageable) {
        return agendamentoService.buscar(dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um agendamento específico")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public AgendamentoDTO buscarPorId(@PathVariable Long id) {
        return agendamentoService.buscarPorId(id);
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Acompanha atualizações em tempo real do agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public SseEmitter acompanhar(@PathVariable Long id) {
        return agendamentoRealtimeService.registrar(id);
    }

    @PostMapping
    @Operation(summary = "Cria um novo agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public ResponseEntity<AgendamentoDTO> criar(@Valid @RequestBody AgendamentoRequest request) {
        AgendamentoDTO criado = agendamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento existente")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public AgendamentoDTO atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoRequest request) {
        return agendamentoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela um agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Realiza upload de documento para o agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public ResponseEntity<DocumentoAgendamentoDTO> uploadDocumento(@PathVariable Long id,
                                                                   @Valid @RequestPart(value = "metadata", required = false) DocumentoUploadRequest metadata,
                                                                   @RequestPart("file") MultipartFile arquivo) {
        DocumentoAgendamentoDTO documento = agendamentoService.adicionarDocumento(id, metadata, arquivo);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documento);
    }

    @GetMapping("/{id}/documentos")
    @Operation(summary = "Lista documentos de um agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public List<DocumentoAgendamentoDTO> listarDocumentos(@PathVariable Long id) {
        return agendamentoService.listarDocumentos(id);
    }

    @GetMapping("/{agendamentoId}/documentos/{documentoId}")
    @Operation(summary = "Download de documento do agendamento")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
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
