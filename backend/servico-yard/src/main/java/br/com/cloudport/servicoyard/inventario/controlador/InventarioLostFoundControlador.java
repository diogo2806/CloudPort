package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.modelo.CasoLostFound;
import br.com.cloudport.servicoyard.inventario.modelo.TipoCasoLostFound;
import br.com.cloudport.servicoyard.inventario.servico.CasoLostFoundServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/casos")
@Tag(name = "Inventário Lost & Found/TBD", description = "Fila operacional de unidades desconhecidas ou não localizadas")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO','PLANEJADOR')")
public class InventarioLostFoundControlador {

    private final CasoLostFoundServico servico;

    public InventarioLostFoundControlador(CasoLostFoundServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @Operation(summary = "Lista a fila de casos")
    public List<Map<String, Object>> listar() {
        return servico.listar().stream().map(this::mapear).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "Abre caso para unidade sem registro, não localizada ou TBD")
    public ResponseEntity<Map<String, Object>> abrir(@Valid @RequestBody AberturaRequest request) {
        return ResponseEntity.ok(mapear(servico.abrir(
                request.identificacaoLida, request.tipoCaso, request.evidencia, request.operador)));
    }

    @PostMapping("/{id}/investigate")
    @Operation(summary = "Atribui responsável e inicia investigação")
    public ResponseEntity<Map<String, Object>> investigar(@PathVariable Long id,
                                                           @Valid @RequestBody InvestigacaoRequest request) {
        return ResponseEntity.ok(mapear(servico.investigar(id, request.responsavel, request.evidencia)));
    }

    @PostMapping("/{id}/associate")
    @Operation(summary = "Associa o caso a uma unidade canônica")
    public ResponseEntity<Map<String, Object>> associar(@PathVariable Long id,
                                                         @Valid @RequestBody AssociacaoRequest request) {
        return ResponseEntity.ok(mapear(servico.associar(id, request.unidadeId, request.evidencia)));
    }

    @PostMapping("/{id}/regularize")
    @Operation(summary = "Regulariza a unidade associada")
    public ResponseEntity<Map<String, Object>> regularizar(@PathVariable Long id,
                                                            @Valid @RequestBody DecisaoRequest request) {
        return ResponseEntity.ok(mapear(servico.regularizar(id, request.decisao)));
    }

    @PostMapping("/{id}/write-off")
    @Operation(summary = "Baixa o caso e inativa a unidade associada quando existente")
    public ResponseEntity<Map<String, Object>> baixar(@PathVariable Long id,
                                                       @Valid @RequestBody DecisaoRequest request) {
        return ResponseEntity.ok(mapear(servico.baixar(id, request.decisao)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Encerra um caso regularizado ou baixado")
    public ResponseEntity<Map<String, Object>> encerrar(@PathVariable Long id,
                                                         @Valid @RequestBody DecisaoRequest request) {
        return ResponseEntity.ok(mapear(servico.encerrar(id, request.decisao)));
    }

    private Map<String, Object> mapear(CasoLostFound caso) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", caso.getId());
        dto.put("identificacaoLida", caso.getIdentificacaoLida());
        dto.put("unidadeId", caso.getUnidade() != null ? caso.getUnidade().getId() : null);
        dto.put("unidadeIdentificacao", caso.getUnidade() != null ? caso.getUnidade().getIdentificacao() : null);
        dto.put("tipoCaso", caso.getTipoCaso());
        dto.put("status", caso.getStatus());
        dto.put("responsavel", caso.getResponsavel());
        dto.put("evidencia", caso.getEvidencia());
        dto.put("decisaoFinal", caso.getDecisaoFinal());
        dto.put("abertoPor", caso.getAbertoPor());
        dto.put("abertoEm", caso.getAbertoEm());
        dto.put("investigacaoIniciadaEm", caso.getInvestigacaoIniciadaEm());
        dto.put("associadaEm", caso.getAssociadaEm());
        dto.put("regularizadaEm", caso.getRegularizadaEm());
        dto.put("baixadaEm", caso.getBaixadaEm());
        dto.put("encerradaEm", caso.getEncerradaEm());
        return dto;
    }

    public static class AberturaRequest {
        @NotBlank @Size(max = 40) public String identificacaoLida;
        @NotNull public TipoCasoLostFound tipoCaso;
        @Size(max = 2000) public String evidencia;
        @NotBlank @Size(max = 120) public String operador;
    }

    public static class InvestigacaoRequest {
        @NotBlank @Size(max = 120) public String responsavel;
        @Size(max = 2000) public String evidencia;
    }

    public static class AssociacaoRequest {
        @NotNull public Long unidadeId;
        @Size(max = 2000) public String evidencia;
    }

    public static class DecisaoRequest {
        @NotBlank @Size(max = 1000) public String decisao;
    }
}
