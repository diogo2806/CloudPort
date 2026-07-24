package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.dto.ResumoAvisosEstivagemPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.EstadoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.HistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.servico.AvisoEstivagemPatioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard/stowage-warnings")
@Tag(name = "Avisos de estivagem do pátio",
        description = "Detecção, atribuição, correção, revalidação, resolução e reabertura de violações físicas")
@PreAuthorize("hasAnyRole('ROOT','ADMIN_PORTO','OPERADOR_PATIO','PLANEJADOR')")
public class AvisoEstivagemPatioController {

    private final AvisoEstivagemPatioServico servico;

    public AvisoEstivagemPatioController(AvisoEstivagemPatioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @Operation(summary = "Lista a fila operacional de avisos")
    public List<Map<String, Object>> listar(
            @RequestParam(required = false) EstadoAvisoEstivagemPatio estado,
            @RequestParam(required = false) SeveridadeAvisoEstivagemPatio severidade,
            @RequestParam(required = false) String responsavel,
            @RequestParam(defaultValue = "false") boolean incluirResolvidos) {
        return servico.listar(estado, severidade, responsavel, incluirResolvidos)
                .stream().map(this::mapear).collect(Collectors.toList());
    }

    @GetMapping("/summary")
    @Operation(summary = "Resume badges ativos por bloco, pilha, posição e unidade")
    public ResumoAvisosEstivagemPatioDto resumo() {
        return servico.resumo();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Consulta o histórico imutável do aviso")
    public List<Map<String, Object>> historico(@PathVariable Long id) {
        return servico.historico(id).stream().map(this::mapearHistorico).collect(Collectors.toList());
    }

    @PostMapping("/scan")
    @Operation(summary = "Varre o inventário físico e sincroniza os avisos")
    public List<Map<String, Object>> varrer(@Valid @RequestBody AtorRequest request) {
        return servico.sincronizarInventario(request.ator)
                .stream().map(this::mapear).collect(Collectors.toList());
    }

    @PostMapping("/unit/{codigoUnidade}/revalidate")
    @Operation(summary = "Revalida todos os avisos de uma unidade")
    public List<Map<String, Object>> revalidarUnidade(@PathVariable String codigoUnidade,
                                                       @Valid @RequestBody AtorRequest request) {
        return servico.reavaliarUnidade(codigoUnidade, request.ator)
                .stream().map(this::mapear).collect(Collectors.toList());
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Atribui responsável e prazo ao aviso")
    public ResponseEntity<Map<String, Object>> atribuir(@PathVariable Long id,
                                                         @Valid @RequestBody AtribuicaoRequest request) {
        return ResponseEntity.ok(mapear(servico.atribuir(
                id, request.responsavel, request.prazo, request.ator)));
    }

    @PostMapping("/{id}/start-correction")
    @Operation(summary = "Registra a ação corretiva e inicia a correção")
    public ResponseEntity<Map<String, Object>> iniciarCorrecao(@PathVariable Long id,
                                                                @Valid @RequestBody CorrecaoRequest request) {
        return ResponseEntity.ok(mapear(servico.iniciarCorrecao(
                id, request.acaoCorretiva, request.evidencia, request.ator)));
    }

    @PostMapping("/{id}/submit-revalidation")
    @Operation(summary = "Conclui a correção e envia o aviso para revalidação")
    public ResponseEntity<Map<String, Object>> enviarRevalidacao(@PathVariable Long id,
                                                                  @Valid @RequestBody EvidenciaRequest request) {
        return ResponseEntity.ok(mapear(servico.enviarParaRevalidacao(
                id, request.evidencia, request.ator)));
    }

    @PostMapping("/{id}/revalidate")
    @Operation(summary = "Revalida a condição física antes de resolver ou reabrir")
    public ResponseEntity<Map<String, Object>> revalidar(@PathVariable Long id,
                                                          @Valid @RequestBody EvidenciaRequest request) {
        return ResponseEntity.ok(mapear(servico.revalidar(id, request.evidencia, request.ator)));
    }

    private Map<String, Object> mapear(AvisoEstivagemPatio aviso) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", aviso.getId());
        dto.put("chaveEstavel", aviso.getChaveEstavel());
        dto.put("codigoUnidade", aviso.getCodigoUnidade());
        dto.put("codigoPosicao", aviso.getCodigoPosicao());
        dto.put("bloco", aviso.getBloco());
        dto.put("linha", aviso.getLinha());
        dto.put("coluna", aviso.getColuna());
        dto.put("camada", aviso.getCamada());
        dto.put("regra", aviso.getRegra());
        dto.put("severidade", aviso.getSeveridade());
        dto.put("estado", aviso.getEstado());
        dto.put("valorObservado", aviso.getValorObservado());
        dto.put("valorEsperado", aviso.getValorEsperado());
        dto.put("acaoSugerida", aviso.getAcaoSugerida());
        dto.put("responsavel", aviso.getResponsavel());
        dto.put("prazo", aviso.getPrazo());
        dto.put("acaoCorretiva", aviso.getAcaoCorretiva());
        dto.put("evidencia", aviso.getEvidencia());
        dto.put("resultadoRevalidacao", aviso.getResultadoRevalidacao());
        dto.put("ocorrencias", aviso.getOcorrencias());
        dto.put("bloqueiaOperacao", aviso.isBloqueiaOperacao());
        dto.put("abertoEm", aviso.getAbertoEm());
        dto.put("ultimaRevalidacaoEm", aviso.getUltimaRevalidacaoEm());
        dto.put("resolvidoEm", aviso.getResolvidoEm());
        dto.put("atualizadoEm", aviso.getAtualizadoEm());
        return dto;
    }

    private Map<String, Object> mapearHistorico(HistoricoAvisoEstivagemPatio historico) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", historico.getId());
        dto.put("evento", historico.getEvento());
        dto.put("estadoAnterior", historico.getEstadoAnterior());
        dto.put("estadoNovo", historico.getEstadoNovo());
        dto.put("ator", historico.getAtor());
        dto.put("detalhes", historico.getDetalhes());
        dto.put("evidencia", historico.getEvidencia());
        dto.put("resultado", historico.getResultado());
        dto.put("ocorridoEm", historico.getOcorridoEm());
        return dto;
    }

    public static class AtorRequest {
        @NotBlank @Size(max = 120) public String ator;
    }

    public static class AtribuicaoRequest {
        @NotBlank @Size(max = 120) public String responsavel;
        public LocalDateTime prazo;
        @NotBlank @Size(max = 120) public String ator;
    }

    public static class CorrecaoRequest {
        @NotBlank @Size(max = 2000) public String acaoCorretiva;
        @Size(max = 2000) public String evidencia;
        @NotBlank @Size(max = 120) public String ator;
    }

    public static class EvidenciaRequest {
        @Size(max = 2000) public String evidencia;
        @NotBlank @Size(max = 120) public String ator;
    }
}
