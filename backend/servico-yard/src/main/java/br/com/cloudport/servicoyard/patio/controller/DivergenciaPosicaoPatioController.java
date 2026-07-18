package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.modelo.DivergenciaPosicaoPatio;
import br.com.cloudport.servicoyard.patio.servico.DivergenciaPosicaoPatioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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
@RequestMapping("/api/yard/position-divergences")
@Tag(name = "Divergências de posição", description = "Investigação, bloqueio e correção de unidades fora de posição")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO','PLANEJADOR')")
public class DivergenciaPosicaoPatioController {

    private final DivergenciaPosicaoPatioServico servico;

    public DivergenciaPosicaoPatioController(DivergenciaPosicaoPatioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @Operation(summary = "Lista casos de divergência")
    public List<Map<String, Object>> listar() {
        return servico.listar().stream().map(this::mapear).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "Abre e bloqueia uma divergência de posição")
    public ResponseEntity<Map<String, Object>> abrir(@Valid @RequestBody AberturaRequest request) {
        return ResponseEntity.ok(mapear(servico.abrir(request.identificacaoUnidade, request.posicaoEsperada,
                request.posicaoEncontrada, request.evidencia, request.operador)));
    }

    @PostMapping("/{id}/investigate")
    @Operation(summary = "Inicia a investigação")
    public ResponseEntity<Map<String, Object>> investigar(@PathVariable Long id,
                                                            @Valid @RequestBody InvestigacaoRequest request) {
        return ResponseEntity.ok(mapear(servico.iniciarInvestigacao(id, request.responsavel, request.evidencia)));
    }

    @PostMapping("/{id}/corrective-instruction")
    @Operation(summary = "Cria instrução corretiva com origem e destino confirmados")
    public ResponseEntity<Map<String, Object>> criarInstrucao(@PathVariable Long id,
                                                                @Valid @RequestBody CorrecaoRequest request) {
        return ResponseEntity.ok(mapear(servico.criarInstrucaoCorretiva(
                id, request.equipamento, request.equipe, request.operador)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve após a conclusão da instrução corretiva")
    public ResponseEntity<Map<String, Object>> resolver(@PathVariable Long id,
                                                          @Valid @RequestBody DecisaoRequest request) {
        return ResponseEntity.ok(mapear(servico.resolver(id, request.decisao)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela o caso e remove o bloqueio")
    public ResponseEntity<Map<String, Object>> cancelar(@PathVariable Long id,
                                                          @Valid @RequestBody DecisaoRequest request) {
        return ResponseEntity.ok(mapear(servico.cancelar(id, request.decisao)));
    }

    private Map<String, Object> mapear(DivergenciaPosicaoPatio caso) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", caso.getId());
        dto.put("unidadeId", caso.getUnidade().getId());
        dto.put("condicaoAnterior", caso.getCondicaoAnterior());
        dto.put("identificacaoUnidade", caso.getIdentificacaoUnidade());
        dto.put("posicaoEsperada", caso.getPosicaoEsperada());
        dto.put("posicaoEncontrada", caso.getPosicaoEncontrada());
        dto.put("status", caso.getStatus());
        dto.put("bloqueada", caso.isBloqueada());
        dto.put("responsavel", caso.getResponsavel());
        dto.put("evidencia", caso.getEvidencia());
        dto.put("decisao", caso.getDecisao());
        dto.put("instrucaoCorretivaId", caso.getInstrucaoCorretiva() != null ? caso.getInstrucaoCorretiva().getId() : null);
        dto.put("abertaPor", caso.getAbertaPor());
        dto.put("abertaEm", caso.getAbertaEm());
        dto.put("investigacaoIniciadaEm", caso.getInvestigacaoIniciadaEm());
        dto.put("resolvidaEm", caso.getResolvidaEm());
        dto.put("canceladaEm", caso.getCanceladaEm());
        return dto;
    }

    public static class AberturaRequest {
        @NotBlank @Size(max = 40) public String identificacaoUnidade;
        @NotBlank @Size(max = 120) public String posicaoEsperada;
        @NotBlank @Size(max = 120) public String posicaoEncontrada;
        @Size(max = 1000) public String evidencia;
        @NotBlank @Size(max = 120) public String operador;
    }

    public static class InvestigacaoRequest {
        @NotBlank @Size(max = 120) public String responsavel;
        @Size(max = 1000) public String evidencia;
    }

    public static class CorrecaoRequest {
        @Size(max = 40) public String equipamento;
        @Size(max = 80) public String equipe;
        @NotBlank @Size(max = 120) public String operador;
    }

    public static class DecisaoRequest {
        @NotBlank @Size(max = 1000) public String decisao;
    }
}
