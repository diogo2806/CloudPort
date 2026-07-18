package br.com.cloudport.servicogate.app.operacional;

import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AdvanceVisitRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AttachmentDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AttachmentRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BookingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BookingRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BusinessTaskDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BusinessTaskRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.DocumentDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.DocumentRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.FacilityDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.FacilityRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.GateDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.GateOperationalDashboardDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.GateRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.InspectionDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.InspectionRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.LaneDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.LaneRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.OrderDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.OrderRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.PreadviceDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.PreadviceRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.ReferenceCatalogDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.StageDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.StageRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TransferDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TransferRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TroubleDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TroubleRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TroubleResolutionRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TruckVisitDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TruckVisitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/operacional")
@Tag(name = "Gate operacional", description = "Book of reference, configurações, truck visits e execução por estágios")
public class GateOperacionalController {

    private final GateOperacionalService service;

    public GateOperacionalController(GateOperacionalService service) {
        this.service = service;
    }

    @GetMapping("/painel")
    @Operation(summary = "Obtém o painel operacional consolidado do Gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public GateOperationalDashboardDTO painel(
            @Parameter(description = "Instalação selecionada") @RequestParam(required = false) Long facilityId) {
        return service.painel(facilityId);
    }

    @GetMapping("/referencias")
    @Operation(summary = "Lista bookings, EDOs, EROs, IDOs e pré-avisos disponíveis")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','TRANSPORTADORA')")
    public ReferenceCatalogDTO referencias() {
        return service.listarReferencias();
    }

    @PostMapping("/configuracao/facilities")
    @Operation(summary = "Cria ou atualiza uma instalação de Gate")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public FacilityDTO salvarFacility(@Valid @RequestBody FacilityRequest request) {
        return service.salvarFacility(request);
    }

    @PostMapping("/configuracao/gates")
    @Operation(summary = "Cria ou atualiza um Gate da instalação")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public GateDTO salvarGate(@Valid @RequestBody GateRequest request) {
        return service.salvarGate(request);
    }

    @PostMapping("/configuracao/lanes")
    @Operation(summary = "Cria ou atualiza uma pista operacional")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public LaneDTO salvarLane(@Valid @RequestBody LaneRequest request) {
        return service.salvarLane(request);
    }

    @PostMapping("/configuracao/stages")
    @Operation(summary = "Cria ou atualiza um estágio configurável do fluxo")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public StageDTO salvarStage(@Valid @RequestBody StageRequest request) {
        return service.salvarStage(request);
    }

    @PostMapping("/configuracao/stages/{stageId}/tasks")
    @Operation(summary = "Cria ou atualiza uma business task do estágio")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public BusinessTaskDTO salvarTask(@PathVariable Long stageId,
                                      @Valid @RequestBody BusinessTaskRequest request) {
        return service.salvarTask(stageId, request);
    }

    @PostMapping("/bookings")
    @Operation(summary = "Cria ou atualiza um booking operacional")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public BookingDTO salvarBooking(@Valid @RequestBody BookingRequest request) {
        return service.salvarBooking(request);
    }

    @PostMapping("/ordens")
    @Operation(summary = "Cria ou atualiza EDO, ERO ou IDO")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public OrderDTO salvarOrder(@Valid @RequestBody OrderRequest request) {
        return service.salvarOrder(request);
    }

    @PostMapping("/pre-avisos")
    @Operation(summary = "Cria ou atualiza pré-aviso de exportação ou vazio")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','TRANSPORTADORA')")
    public PreadviceDTO salvarPreadvice(@Valid @RequestBody PreadviceRequest request) {
        return service.salvarPreadvice(request);
    }

    @PostMapping("/visitas")
    @Operation(summary = "Abre truck visit com uma ou múltiplas transações")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<TruckVisitDTO> criarVisita(@Valid @RequestBody TruckVisitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarVisita(request));
    }

    @GetMapping("/visitas/{visitaId}")
    @Operation(summary = "Detalha a truck visit e suas transações")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public TruckVisitDTO buscarVisita(@PathVariable Long visitaId) {
        return service.buscarVisita(visitaId);
    }

    @PostMapping("/visitas/{visitaId}/avancar")
    @Operation(summary = "Executa business tasks e avança a truck visit para o próximo estágio")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public TruckVisitDTO avancarVisita(@PathVariable Long visitaId,
                                       @Valid @RequestBody AdvanceVisitRequest request) {
        return service.avancarVisita(visitaId, request);
    }

    @PostMapping("/transacoes/{transactionId}/troubles")
    @Operation(summary = "Abre trouble transaction e retém a truck visit")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<TroubleDTO> abrirTrouble(@PathVariable Long transactionId,
                                                   @Valid @RequestBody TroubleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.abrirTrouble(transactionId, request));
    }

    @PostMapping("/troubles/{troubleId}/resolver")
    @Operation(summary = "Resolve trouble transaction e libera a continuidade do fluxo")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public TroubleDTO resolverTrouble(@PathVariable Long troubleId,
                                      @Valid @RequestBody TroubleResolutionRequest request) {
        return service.resolverTrouble(troubleId, request);
    }

    @PostMapping("/transacoes/{transactionId}/inspecoes")
    @Operation(summary = "Registra inspeção; reprovação abre trouble automaticamente")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<InspectionDTO> registrarInspecao(@PathVariable Long transactionId,
                                                           @Valid @RequestBody InspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarInspecao(transactionId, request));
    }

    @PostMapping("/anexos")
    @Operation(summary = "Vincula fotografia, avaria, OCR ou documento à operação")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<AttachmentDTO> anexar(@Valid @RequestBody AttachmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.anexar(request));
    }

    @PostMapping("/visitas/{visitaId}/documentos")
    @Operation(summary = "Emite ticket, EIR ou comprovante de transferência")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public ResponseEntity<DocumentDTO> emitirDocumento(@PathVariable Long visitaId,
                                                        @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.emitirDocumento(visitaId, request));
    }

    @PostMapping("/documentos/{documentoId}/reimprimir")
    @Operation(summary = "Registra e retorna a reimpressão de documento de Gate")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public DocumentDTO reimprimirDocumento(@PathVariable Long documentoId) {
        return service.reimprimirDocumento(documentoId);
    }

    @PostMapping("/visitas/{visitaId}/transferencias")
    @Operation(summary = "Solicita transferência da truck visit entre instalações")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<TransferDTO> solicitarTransferencia(@PathVariable Long visitaId,
                                                               @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarTransferencia(visitaId, request));
    }

    @PostMapping("/transferencias/{transferenciaId}/receber")
    @Operation(summary = "Recebe a transferência e reinicia o fluxo no Gate de destino")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public TransferDTO receberTransferencia(@PathVariable Long transferenciaId) {
        return service.receberTransferencia(transferenciaId);
    }
}