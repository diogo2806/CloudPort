package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.GateResourceOccupation;
import br.com.cloudport.servicogate.model.enums.GateResourceType;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gate/truck-hopping")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
@Hidden
@Transactional
public class GateTruckHoppingCompatibilityController {

    private final GatePassRepository gatePassRepository;
    private final GateResourceOccupationRepository occupationRepository;
    private final GateResourceOccupationService occupationService;

    public GateTruckHoppingCompatibilityController(GatePassRepository gatePassRepository,
                                                    GateResourceOccupationRepository occupationRepository,
                                                    GateResourceOccupationService occupationService) {
        this.gatePassRepository = gatePassRepository;
        this.occupationRepository = occupationRepository;
        this.occupationService = occupationService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar() {
        return occupationRepository.findAll().stream()
                .filter(GateResourceOccupation::isAtivo)
                .filter(item -> item.getTipoRecurso() == GateResourceType.MOTORISTA)
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> abrir(@Valid @RequestBody CompatibilityRequest request) {
        GatePass gatePass = obterGatePass(request.getGatePassId());
        occupationService.ocuparRecursos(gatePass.getAgendamento(), gatePass, null, List.of());
        GateResourceOccupation motorista = occupationRepository
                .findFirstByTipoRecursoAndChaveRecursoAndAtivoTrue(
                        GateResourceType.MOTORISTA,
                        normalizar(gatePass.getAgendamento().getMotorista().getDocumento()))
                .orElseThrow(() -> new NotFoundException("Ocupação do motorista não foi criada"));
        return ResponseEntity.ok(mapear(motorista));
    }

    @PostMapping("/{cpf}/close")
    public ResponseEntity<Map<String, Object>> encerrar(@PathVariable String cpf,
                                                         @Valid @RequestBody CloseCompatibilityRequest request) {
        GateResourceOccupation motorista = occupationRepository
                .findFirstByTipoRecursoAndChaveRecursoAndAtivoTrue(
                        GateResourceType.MOTORISTA, normalizar(cpf))
                .orElseThrow(() -> new NotFoundException("Visita ativa não encontrada para o motorista"));
        occupationService.liberarRecursos(motorista.getGatePass().getId());
        Map<String, Object> resultado = mapear(motorista);
        resultado.put("status", "ENCERRADA");
        resultado.put("gateOutId", request.getGatePassId());
        resultado.put("cavaloAtual", request.getCavaloAtual());
        return ResponseEntity.ok(resultado);
    }

    private GatePass obterGatePass(Long id) {
        return gatePassRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("GatePass não encontrado"));
    }

    private Map<String, Object> mapear(GateResourceOccupation motorista) {
        GatePass gatePass = motorista.getGatePass();
        String cavalo = occupationRepository.findByGatePassIdAndAtivoTrue(gatePass.getId()).stream()
                .filter(item -> item.getTipoRecurso() == GateResourceType.CAVALO)
                .map(GateResourceOccupation::getChaveRecurso)
                .findFirst()
                .orElse(null);
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", motorista.getId());
        dto.put("cpfMotorista", motorista.getChaveRecurso());
        dto.put("numeroCnh", gatePass.getAgendamento().getMotorista().getNumeroCnh());
        dto.put("cavaloAtual", cavalo);
        dto.put("status", motorista.isAtivo() ? "ABERTA" : "ENCERRADA");
        dto.put("gateInId", gatePass.getId());
        dto.put("gateOutId", motorista.isAtivo() ? null : gatePass.getId());
        dto.put("abertaEm", motorista.getOcupadoEm());
        dto.put("encerradaEm", motorista.getLiberadoEm());
        return dto;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public static class CompatibilityRequest {
        @NotBlank private String cpfMotorista;
        @NotBlank private String numeroCnh;
        @NotBlank private String cavaloAtual;
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

    public static class CloseCompatibilityRequest {
        @NotBlank private String cavaloAtual;
        @NotNull private Long gatePassId;
        public String getCavaloAtual() { return cavaloAtual; }
        public void setCavaloAtual(String cavaloAtual) { this.cavaloAtual = cavaloAtual; }
        public Long getGatePassId() { return gatePassId; }
        public void setGatePassId(Long gatePassId) { this.gatePassId = gatePassId; }
    }
}
