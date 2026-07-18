package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.GateCall;
import br.com.cloudport.servicogate.model.GateQueueEntry;
import br.com.cloudport.servicogate.model.TruckHoppingSession;
import br.com.cloudport.servicogate.model.enums.GateCallPriority;
import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import br.com.cloudport.servicogate.model.enums.GateQueuePriority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gate")
@Tag(name = "Operações de Gate", description = "Troca de cavalo, chamados e filas operacionais")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
public class GateOperationsController {

    private final GateOperationsService service;

    public GateOperationsController(GateOperationsService service) {
        this.service = service;
    }

    @GetMapping("/truck-hopping")
    @Operation(summary = "Lista sessões de troca de cavalo")
    public List<Map<String, Object>> listarTrocas() {
        return service.listarTrocas().stream().map(this::mapearTroca).collect(Collectors.toList());
    }

    @PostMapping("/truck-hopping")
    @Operation(summary = "Abre uma sessão de troca de cavalo")
    public ResponseEntity<Map<String, Object>> abrirTroca(@Valid @RequestBody TruckHoppingRequest request) {
        return ResponseEntity.ok(mapearTroca(service.abrirTroca(
                request.getCpfMotorista(), request.getNumeroCnh(), request.getCavaloAtual(), request.getGatePassId())));
    }

    @PostMapping("/truck-hopping/{cpf}/close")
    @Operation(summary = "Encerra a sessão aberta de troca de cavalo")
    public ResponseEntity<Map<String, Object>> encerrarTroca(@PathVariable String cpf,
                                                              @Valid @RequestBody CloseTruckHoppingRequest request) {
        return ResponseEntity.ok(mapearTroca(service.encerrarTroca(cpf, request.getCavaloAtual(), request.getGatePassId())));
    }

    @GetMapping("/calls")
    @Operation(summary = "Lista chamados do gate")
    public List<Map<String, Object>> listarChamados() {
        return service.listarChamados().stream().map(this::mapearChamado).collect(Collectors.toList());
    }

    @PostMapping("/calls")
    @Operation(summary = "Chama um veículo para atendimento")
    public ResponseEntity<Map<String, Object>> chamar(@Valid @RequestBody CallRequest request) {
        return ResponseEntity.ok(mapearChamado(service.chamarVeiculo(
                request.getGatePassId(), request.getPrioridade(), request.getOperador())));
    }

    @PostMapping("/calls/{id}/start")
    @Operation(summary = "Inicia o atendimento de um chamado")
    public ResponseEntity<Map<String, Object>> iniciar(@PathVariable Long id) {
        return ResponseEntity.ok(mapearChamado(service.iniciarAtendimento(id)));
    }

    @PostMapping("/calls/{id}/finish")
    @Operation(summary = "Finaliza o atendimento de um chamado")
    public ResponseEntity<Map<String, Object>> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(mapearChamado(service.finalizarAtendimento(id)));
    }

    @PostMapping("/calls/{id}/cancel")
    @Operation(summary = "Cancela um chamado ativo")
    public ResponseEntity<Map<String, Object>> cancelar(@PathVariable Long id,
                                                         @Valid @RequestBody CancellationRequest request) {
        return ResponseEntity.ok(mapearChamado(service.cancelar(id, request.getJustificativa())));
    }

    @GetMapping("/queues")
    @Operation(summary = "Consulta a fila ordenada por prioridade e FIFO")
    public List<Map<String, Object>> listarFila(@RequestParam(defaultValue = "ENTRADA") GateQueueDirection sentido) {
        return service.listarFila(sentido).stream().map(this::mapearFila).collect(Collectors.toList());
    }

    @PostMapping("/queues")
    @Operation(summary = "Inclui um GatePass na fila")
    public ResponseEntity<Map<String, Object>> adicionarFila(@Valid @RequestBody QueueRequest request) {
        return ResponseEntity.ok(mapearFila(service.adicionarNaFila(request.getGatePassId(), request.getSentido())));
    }

    @PatchMapping("/queues/{id}/position")
    @Operation(summary = "Reordena um item preservando sua posição original")
    public ResponseEntity<Map<String, Object>> reordenar(@PathVariable Long id,
                                                          @Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(mapearFila(service.reordenar(
                id, request.getPosicao(), request.getJustificativa(), request.getOperador())));
    }

    @PatchMapping("/queues/{id}/priority")
    @Operation(summary = "Altera a prioridade de um item da fila")
    public ResponseEntity<Map<String, Object>> alterarPrioridade(@PathVariable Long id,
                                                                  @Valid @RequestBody PriorityRequest request) {
        return ResponseEntity.ok(mapearFila(service.alterarPrioridade(
                id, request.getPrioridade(), request.getJustificativa(), request.getOperador())));
    }

    private Map<String, Object> mapearTroca(TruckHoppingSession sessao) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", sessao.getId());
        dto.put("cpfMotorista", sessao.getCpfMotorista());
        dto.put("numeroCnh", sessao.getNumeroCnh());
        dto.put("cavaloAtual", sessao.getCavaloAtual());
        dto.put("status", sessao.getStatus());
        dto.put("gateInId", sessao.getGateIn() != null ? sessao.getGateIn().getId() : null);
        dto.put("gateOutId", sessao.getGateOut() != null ? sessao.getGateOut().getId() : null);
        dto.put("abertaEm", sessao.getCreatedAt());
        dto.put("encerradaEm", sessao.getEncerradaEm());
        return dto;
    }

    private Map<String, Object> mapearChamado(GateCall chamado) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", chamado.getId());
        dto.put("gatePassId", chamado.getGatePass().getId());
        dto.put("codigoGatePass", chamado.getGatePass().getCodigo());
        dto.put("status", chamado.getStatus());
        dto.put("prioridade", chamado.getPrioridade());
        dto.put("chamadoEm", chamado.getChamadoEm());
        dto.put("atendimentoIniciadoEm", chamado.getAtendimentoIniciadoEm());
        dto.put("finalizadoEm", chamado.getFinalizadoEm());
        dto.put("canceladoEm", chamado.getCanceladoEm());
        dto.put("justificativaCancelamento", chamado.getJustificativaCancelamento());
        dto.put("operador", chamado.getOperador());
        return dto;
    }

    private Map<String, Object> mapearFila(GateQueueEntry entrada) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", entrada.getId());
        dto.put("gatePassId", entrada.getGatePass().getId());
        dto.put("codigoGatePass", entrada.getGatePass().getCodigo());
        dto.put("sentido", entrada.getSentido());
        dto.put("status", entrada.getStatus());
        dto.put("posicaoOriginal", entrada.getPosicaoOriginal());
        dto.put("posicaoAtual", entrada.getPosicaoAtual());
        dto.put("prioridade", entrada.getPrioridade());
        dto.put("justificativaPrioridade", entrada.getJustificativaPrioridade());
        dto.put("operadorPrioridade", entrada.getOperadorPrioridade());
        dto.put("entrouEm", entrada.getEntrouEm());
        dto.put("chamadoEm", entrada.getChamadoEm());
        dto.put("atendimentoIniciadoEm", entrada.getAtendimentoIniciadoEm());
        return dto;
    }

    public static class TruckHoppingRequest {
        @NotBlank @Schema(example = "123.456.789-00") private String cpfMotorista;
        @NotBlank @Schema(example = "01234567890") private String numeroCnh;
        @NotBlank @Schema(example = "ABC1D23") private String cavaloAtual;
        @NotNull private Long gatePassId;
        public String getCpfMotorista() { return cpfMotorista; }
        public void setCpfMotorista(String cpfMotorista) { this.cpfMotorista = cpfMotorista; }
        public String getNumeroCnh() { return numeroCnh; }
        public void setNumeroCnh(String numeroCnh) { this.numeroCnh = numeroCnh; }
        public String getCavaloAtual() { return cavaloAtual; }
        public void setCavaloAtual(String cavaloAtual) { this.cavaloAtual = cavaloAtual; }
        public Long getGatePassId() { return gatePassId; }
        public void setGatePassId(Long gatePassId) { this.gatePassId = gatePassId; }
    }

    public static class CloseTruckHoppingRequest {
        @NotBlank private String cavaloAtual;
        @NotNull private Long gatePassId;
        public String getCavaloAtual() { return cavaloAtual; }
        public void setCavaloAtual(String cavaloAtual) { this.cavaloAtual = cavaloAtual; }
        public Long getGatePassId() { return gatePassId; }
        public void setGatePassId(Long gatePassId) { this.gatePassId = gatePassId; }
    }

    public static class CallRequest {
        @NotNull private Long gatePassId;
        private GateCallPriority prioridade = GateCallPriority.NORMAL;
        private String operador;
        public Long getGatePassId() { return gatePassId; }
        public void setGatePassId(Long gatePassId) { this.gatePassId = gatePassId; }
        public GateCallPriority getPrioridade() { return prioridade; }
        public void setPrioridade(GateCallPriority prioridade) { this.prioridade = prioridade; }
        public String getOperador() { return operador; }
        public void setOperador(String operador) { this.operador = operador; }
    }

    public static class CancellationRequest {
        @NotBlank private String justificativa;
        public String getJustificativa() { return justificativa; }
        public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    }

    public static class QueueRequest {
        @NotNull private Long gatePassId;
        @NotNull private GateQueueDirection sentido;
        public Long getGatePassId() { return gatePassId; }
        public void setGatePassId(Long gatePassId) { this.gatePassId = gatePassId; }
        public GateQueueDirection getSentido() { return sentido; }
        public void setSentido(GateQueueDirection sentido) { this.sentido = sentido; }
    }

    public static class ReorderRequest {
        @NotNull @Min(1) private Integer posicao;
        @NotBlank private String justificativa;
        private String operador;
        public Integer getPosicao() { return posicao; }
        public void setPosicao(Integer posicao) { this.posicao = posicao; }
        public String getJustificativa() { return justificativa; }
        public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
        public String getOperador() { return operador; }
        public void setOperador(String operador) { this.operador = operador; }
    }

    public static class PriorityRequest {
        @NotNull private GateQueuePriority prioridade;
        private String justificativa;
        private String operador;
        public GateQueuePriority getPrioridade() { return prioridade; }
        public void setPrioridade(GateQueuePriority prioridade) { this.prioridade = prioridade; }
        public String getJustificativa() { return justificativa; }
        public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
        public String getOperador() { return operador; }
        public void setOperador(String operador) { this.operador = operador; }
    }
}
